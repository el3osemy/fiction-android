package at.yawk.fiction.android.storage;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.Story;
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

/**
 * @author yawkat
 */
@Slf4j
// for performance, never give this class an equals/hashcode implementation.
// there should only be one instance per story anyway.
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class StoryWrapper {
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

    @Getter int downloadedChapterCount = -1;
    @Getter int readChapterCount = -1;
    @Getter AndroidFictionProvider provider;

    StoryWrapper() {
        // required for dagger
        throw new UnsupportedOperationException();
    }

    public Story getStory() {
        Story story = data.story;
        if (story == null) { throw new IllegalStateException(); }
        return story;
    }

    public void updateStory(Story changes) {
        lock.writeLock().lock();
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
            boolean updated = !merged.equals(data.story);
            if (merged.getChapters() != null) {
                List<? extends Chapter> chapters = merged.getChapters();
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
                        chapter.setText(null);
                        updated = true;
                    }
                }
            }
            if (!updated) { return; }
            data.story = merged;
            log.trace("saving");
            bakeDownloadedChapterCount();
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setChapterRead(int index, boolean read) {
        ChapterData holder = getChapterHolder(index);
        if (holder == null || holder.textHash == null) { return; }
        String newHash = read ? holder.textHash : null;
        lock.writeLock().lock();
        try {
            if (!Objects.equal(holder.readHash, newHash)) {
                holder.readHash = newHash;
                bakeReadChapterCount();
                save();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void save() {
        lock.readLock().lock();
        try {
            objectStorageManager.save(data, objectId);
        } finally {
            lock.readLock().unlock();
        }
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
            for (int i = 0; i < getStory().getChapters().size(); i++) {
                if (isChapterRead(i)) {
                    readChapterCount++;
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
            int chapterCount = getStory().getChapters().size();
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

    public boolean isChapterRead(int index) {
        ChapterData holder = getChapterHolder(index);
        if (holder == null) { return false; }
        String textHash = holder.textHash;
        return textHash != null && textHash.equals(holder.readHash);
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

    @Data
    static final class StoryData {
        @Nullable Story story;
        List<ChapterData> chapterHolders = new ArrayList<>();

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
