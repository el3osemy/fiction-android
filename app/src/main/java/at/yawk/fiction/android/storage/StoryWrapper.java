package at.yawk.fiction.android.storage;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.ProgressStatus;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.event.StoryUpdateEvent;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.Instant;

/**
 * @author yawkat
 */
@Slf4j
// for performance, never give this class an equals/hashcode implementation.
// there should only be one instance per story anyway.
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StoryWrapper {
    private final Transaction saveTransaction = new Transaction(this::save);

    final String objectId;
    @GuardedBy("lock")
    final StoryData data;

    final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Inject StorageManager storageManager;
    @Inject ObjectStorageManager objectStorageManager;
    @Inject TextStorage textStorage;
    @Inject PojoMerger pojoMerger;
    @Inject EventBus eventBus;
    @Inject ProviderManager providerManager;
    @Inject Index index;

    @Getter int downloadedChapterCount = -1;
    @Getter int readChapterCount = -1;
    @Getter AndroidFictionProvider provider;

    StoryWrapper() {
        // required for dagger
        throw new UnsupportedOperationException();
    }

    /**
     * Get a unique id of this story.
     *
     * @see StorageManager#getStory(String)
     */
    public String getId() {
        return objectId;
    }

    StoryIndexEntry createIndexEntry() throws IllegalStateException {
        StoryIndexEntry entry = new StoryIndexEntry();
        List<? extends Chapter> chapters = getStory().getChapters();
        entry.setProviderId(provider.getId());
        entry.setTotalChapterCount(chapters == null ? 0 : chapters.size());
        entry.setReadChapterCount(readChapterCount);
        entry.setDownloadedChapterCount(downloadedChapterCount);
        return entry;
    }

    /**
     * @return {@code true} if story data (beyond ID) is present, {@code false} otherwise.
     */
    public boolean hasData() {
        return data.story != null;
    }

    public Story getStory() {
        Story story = data.story;
        if (story == null) { throw new IllegalStateException("Story not loaded"); }
        return story;
    }

    public void updateStory(Story changes) {
        lock.writeLock().lock();
        saveTransaction.open();
        try {
            if (provider == null) {
                provider = providerManager.getProvider(changes);
            }

            Story merged = data.story == null ? changes : pojoMerger.merge(changes, data.story);
            if (log.isTraceEnabled()) {
                log.trace("Merging {} -> {} = {}",
                          changes.hashCode(),
                          data.story == null ? 0 : data.story.hashCode(),
                          merged.hashCode());
                log.trace("{} -> {} = {}",
                          System.identityHashCode(changes),
                          System.identityHashCode(data.story),
                          System.identityHashCode(merged));
            }
            boolean updatedChapterText = false;
            List<? extends Chapter> chapters = merged.getChapters();
            if (chapters != null) {
                for (int i = 0; i < chapters.size(); i++) {
                    Chapter chapter = chapters.get(i);
                    FormattedText text = chapter.getText();
                    if (text != null) {
                        ChapterData holder = getOrCreateChapterHolder(i);
                        String hash = textStorage.externalizeText(text);
                        holder.setTextHash(hash);
                        //noinspection deprecation
                        if (data.readChapters.contains(text)) {
                            holder.setReadHash(hash);
                        }
                        updatedChapterText |= !hash.equals(holder.getTextHash());
                        chapter.setText(null);
                    }
                }
            }
            if (updatedChapterText || !merged.equals(data.story)) {
                data.story = merged;
                log.trace("saving");
                bakeDownloadedChapterCount();
                bakeReadChapterCount();
                if (updatedChapterText) {
                    bumpLastActionTime();
                }
                saveTransaction.requestSave();
            }
        } finally {
            saveTransaction.commit();
            lock.writeLock().unlock();
        }
    }

    /**
     * Mark this chapter as read. This may be a blocking operation.
     */
    public void setChapterRead(int index, boolean read) throws Exception {
        if (provider.useProvidedReadStatus()) {
            provider.setRead(getStory(), getStory().getChapters().get(index), read);
            saveTransaction.open();
            try {
                bakeReadChapterCount();
                bumpLastActionTime();
                saveTransaction.requestSave();
            } finally {
                saveTransaction.commit();
            }
        } else {
            ChapterData holder = getChapterHolder(index);
            if (holder == null || holder.textHash == null) { return; }
            String newHash = read ? holder.textHash : null;
            lock.writeLock().lock();
            saveTransaction.open();
            try {
                if (!Objects.equal(holder.readHash, newHash)) {
                    holder.readHash = newHash;
                    bakeReadChapterCount();
                    bumpLastActionTime();
                    saveTransaction.requestSave();
                }
            } finally {
                saveTransaction.commit();
                lock.writeLock().unlock();
            }
        }
    }

    private void save() {
        lock.readLock().lock();
        try {
            objectStorageManager.save(data, objectId);
        } finally {
            lock.readLock().unlock();
        }
        index.invalidate(this);
        postUpdateEvent();
    }

    private void postUpdateEvent() {
        eventBus.post(new StoryUpdateEvent(this));
    }

    @Nullable
    private ChapterData getChapterHolder(int index) {
        return data.chapterHolders.size() > index ? data.chapterHolders.get(index) : null;
    }

    private ChapterData getOrCreateChapterHolder(int index) {
        // this relies on chapterHolders never shrinking to maintain thread safety

        if (data.chapterHolders.size() <= index) {
            lock.writeLock().lock();
            try {
                do {
                    data.chapterHolders.add(new ChapterData());
                } while (data.chapterHolders.size() <= index);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return data.chapterHolders.get(index);
    }

    void bakeReadChapterCount() {
        lock.readLock().lock();
        try {
            int readChapterCount = 0;
            List<? extends Chapter> chapters = getStory().getChapters();
            if (chapters != null) {
                for (int i = 0; i < chapters.size(); i++) {
                    if (Boolean.TRUE.equals(isChapterRead(i))) {
                        readChapterCount++;
                    }
                }
            }
            this.readChapterCount = readChapterCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    void bakeDownloadedChapterCount() {
        lock.readLock().lock();
        try {
            List<? extends Chapter> chapters = getStory().getChapters();
            int chapterCount = chapters == null ? 0 : chapters.size();
            int downloadedChapterCount = 0;
            for (int i = 0; i < chapterCount; i++) {
                if (isChapterDownloaded(i)) {
                    downloadedChapterCount++;
                }
            }
            this.downloadedChapterCount = downloadedChapterCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return {@code null} if undefined.
     */
    @Nullable
    public Boolean isChapterRead(int index) {
        if (provider.useProvidedReadStatus()) {
            return getStory().getChapters().get(index).getRead();
        } else {
            ChapterData holder = getChapterHolder(index);
            if (holder == null) { return null; }
            String textHash = holder.textHash;
            if (textHash == null) { return null; }
            return textHash.equals(holder.readHash);
        }
    }

    public boolean isChapterDownloaded(int index) {
        ChapterData holder = getChapterHolder(index);
        return holder != null && holder.textHash != null;
    }

    @Nullable
    public FormattedText loadChapterText(int index) {
        ChapterData holder = getChapterHolder(index);
        if (holder == null) { return null; }
        String hash = holder.textHash;
        return hash == null ? null : textStorage.getText(hash);
    }

    @Nullable
    public String getSavedTextHash(int index) {
        ChapterData holder = getChapterHolder(index);
        return holder == null ? null : holder.textHash;
    }

    public void setDownloading(int chapterIndex, boolean downloading) {
        getOrCreateChapterHolder(chapterIndex).downloading = downloading;
        postUpdateEvent();
    }

    public boolean isDownloading(int chapterIndex) {
        ChapterData holder = getChapterHolder(chapterIndex);
        return holder != null && holder.downloading;
    }

    public ProgressStatus getReadProgressType() {
        List<? extends Chapter> chapters = getStory().getChapters();
        return ProgressStatus.of(getReadChapterCount(), chapters == null ? 0 : chapters.size());
    }

    public ProgressStatus getDownloadProgressType() {
        return ProgressStatus.of(getDownloadedChapterCount(), getStory().getChapters().size());
    }

    @Nullable
    public Instant getLastActionTime() {
        return data.getLastActionTime();
    }

    public void bumpLastActionTime() {
        setLastActionTime(Instant.now());
    }

    public void setLastActionTime(Instant lastOpenTime) {
        saveTransaction.open();
        try {
            data.setLastActionTime(lastOpenTime);
            saveTransaction.requestSave();
        } finally {
            saveTransaction.commit();
        }
    }

    @Data
    static final class StoryData {
        @Nullable Story story;
        List<ChapterData> chapterHolders = new ArrayList<>();
        /**
         * The last time this story was opened or a chapter was downloaded.
         */
        @Nullable Instant lastActionTime;

        @Deprecated
        @JsonProperty
        Set<FormattedText> readChapters = new HashSet<>();
    }

    @Data
    static final class ChapterData {
        @Nullable String textHash;
        @Nullable String readHash;

        @JsonIgnore transient boolean downloading;
    }
}
