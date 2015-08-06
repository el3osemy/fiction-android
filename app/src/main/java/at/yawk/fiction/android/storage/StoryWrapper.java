package at.yawk.fiction.android.storage;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.event.StoryUpdateEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
// for performance, never give this class an equals/hashcode implementation.
// there should only be one instance per story anyway.
public class StoryWrapper {
    transient String objectId;

    @Inject transient StorageManager storageManager;
    @Inject transient ObjectStorageManager objectStorageManager;
    @Inject transient TextStorage textStorage;
    @Inject transient PojoMerger pojoMerger;
    transient EventBus eventBus;

    @JsonProperty private Story story;

    @Deprecated
    @JsonProperty
    private Set<FormattedText> readChapters = new HashSet<>();

    @JsonProperty private List<ChapterHolder> chapterHolders = new ArrayList<>();

    private transient int downloadedChapterCount = -1;
    private transient int readChapterCount = -1;

    public Story getStory() {
        return story;
    }

    @JsonIgnore
    String getObjectId() {
        if (objectId == null) {
            objectId = storageManager.getObjectId(story);
        }
        return objectId;
    }

    public synchronized void updateStory(Story changes) {
        Story merged = this.story == null ? changes : pojoMerger.merge(changes, this.story);
        if (log.isTraceEnabled()) {
            log.trace("Merging {} -> {} = {}",
                      changes.hashCode(),
                      story == null ? 0 : story.hashCode(),
                      merged.hashCode());
            log.trace("{} -> {} = {}",
                      System.identityHashCode(changes),
                      System.identityHashCode(story),
                      System.identityHashCode(merged));
        }
        boolean updated = !merged.equals(story);
        if (merged.getChapters() != null) {
            List<? extends Chapter> chapters = merged.getChapters();
            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                FormattedText text = chapter.getText();
                if (text != null) {
                    ChapterHolder holder = getChapterHolder(i);
                    String hash = textStorage.externalizeText(text);
                    holder.setTextHash(hash);
                    if (readChapters.contains(text)) {
                        holder.setReadHash(hash);
                    }
                    chapter.setText(null);
                    updated = true;
                }
            }
        }
        if (!updated) { return; }
        story = merged;
        log.trace("saving");
        bakeDownloadedChapterCount();
        save();
    }

    public synchronized void setChapterRead(int index, boolean read) {
        ChapterHolder holder = getChapterHolder(index);
        if (holder.getTextHash() == null) { return; }
        String newHash = read ? holder.textHash : null;
        if (!Objects.equal(holder.readHash, newHash)) {
            holder.readHash = newHash;
            bakeReadChapterCount();
            save();
        }
    }

    private synchronized void save() {
        objectStorageManager.save(this, getObjectId());
        postUpdateEvent();
    }

    private void postUpdateEvent() {
        eventBus.post(new StoryUpdateEvent(this));
    }

    @JsonIgnore
    public int getReadChapterCount() {
        if (readChapterCount == -1) {
            bakeReadChapterCount();
        }
        return readChapterCount;
    }

    @JsonIgnore
    public synchronized int getDownloadedCount() {
        if (downloadedChapterCount == -1) {
            bakeDownloadedChapterCount();
        }
        return downloadedChapterCount;
    }

    private synchronized ChapterHolder getChapterHolder(int index) {
        while (chapterHolders.size() <= index) {
            chapterHolders.add(new ChapterHolder());
        }
        return chapterHolders.get(index);
    }

    private synchronized void bakeReadChapterCount() {
        readChapterCount = 0;
        for (int i = 0; i < story.getChapters().size(); i++) {
            if (isChapterRead(i)) {
                readChapterCount++;
            }
        }
    }

    private synchronized void bakeDownloadedChapterCount() {
        int chapterCount = getStory().getChapters().size();
        downloadedChapterCount = 0;
        for (int i = 0; i < chapterCount; i++) {
            if (hasChapterText(i)) {
                downloadedChapterCount++;
            }
        }
    }

    public synchronized boolean isChapterRead(int index) {
        ChapterHolder holder = getChapterHolder(index);
        return holder.textHash != null && Objects.equal(holder.readHash, holder.textHash);
    }

    public synchronized boolean hasChapterText(int index) {
        return getChapterHolder(index).textHash != null;
    }

    @Nullable
    public synchronized FormattedText loadChapterText(int index) {
        String hash = getChapterHolder(index).textHash;
        return hash == null ? null : textStorage.getText(hash);
    }

    public synchronized String getSavedTextHash(int index) {
        return getChapterHolder(index).textHash;
    }

    public synchronized void setDownloading(int chapterIndex, boolean downloading) {
        getChapterHolder(chapterIndex).setDownloading(downloading);
        postUpdateEvent();
    }

    public synchronized boolean isDownloading(int chapterIndex) {
        return getChapterHolder(chapterIndex).isDownloading();
    }

    @Data
    private static final class ChapterHolder {
        @Nullable String textHash;
        @Nullable String readHash;
        @JsonIgnore transient boolean downloading;
    }
}
