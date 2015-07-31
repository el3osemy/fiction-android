package at.yawk.fiction.android.storage;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.Story;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ToString(exclude = "manager", doNotUseGetters = true)
public class StoryWrapper {
    transient String objectId;
    transient StorageManager manager;

    @JsonProperty private Story story;

    @Deprecated
    @JsonProperty
    private Set<FormattedText> readChapters = new HashSet<>();

    @JsonProperty private List<ChapterHolder> chapterHolders = new ArrayList<>();
    private transient int readChapterCount = -1;

    public Story getStory() {
        return story;
    }

    @JsonIgnore
    String getObjectId() {
        if (objectId == null) {
            objectId = manager.getObjectId(story);
        }
        return objectId;
    }

    public synchronized void updateStory(Story changes) {
        log.trace("merging");
        Story merged = this.story == null ? changes : manager.pojoMerger.merge(changes, this.story);
        if (merged.equals(story)) { return; }
        if (merged.getChapters() != null) {
            List<? extends Chapter> chapters = merged.getChapters();
            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                FormattedText text = chapter.getText();
                if (text != null) {
                    ChapterHolder holder = getChapterHolder(i);
                    String hash = manager.getTextStorage().externalizeText(text);
                    holder.setTextHash(hash);
                    if (readChapters.contains(text)) {
                        holder.setReadHash(hash);
                    }
                    chapter.setText(null);
                }
            }
        }
        story = merged;
        log.trace("saving");
        save();
    }

    public synchronized void setChapterRead(int index, boolean read) {
        ChapterHolder holder = getChapterHolder(index);
        if (holder.getTextHash() == null) { return; }
        String newHash = read ? holder.textHash : null;
        if (!Objects.equal(holder.readHash, newHash)) {
            holder.readHash = newHash;
            save();
        }
    }

    private synchronized void save() {
        manager.objectStorage.save(this, getObjectId());
    }

    @JsonIgnore
    public int getReadChapterCount() {
        if (readChapterCount == -1) {
            bakeReadChapterCount();
        }
        return readChapterCount;
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

    public synchronized boolean isChapterRead(int index) {
        ChapterHolder holder = getChapterHolder(index);
        return holder.textHash != null && Objects.equal(holder.readHash, holder.textHash);
    }

    public synchronized boolean hasChapterText(int index) {
        return getChapterHolder(index).textHash != null;
    }

    public synchronized FormattedText loadChapterText(int index) {
        return manager.getTextStorage().getText(getChapterHolder(index).textHash);
    }

    @Data
    private static final class ChapterHolder {
        @Nullable String textHash;
        @Nullable String readHash;
    }
}
