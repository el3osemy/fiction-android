package at.yawk.fiction.android.storage;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.Story;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
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
    /**
     * Chapter text objects that are read in this story
     */
    @JsonProperty private Set<FormattedText> readChapters = new HashSet<>();

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
        story = merged;
        if (changes.getChapters() != null) {
            log.trace("checking chapters");
            Set<FormattedText> read = new HashSet<>();
            for (Chapter chapter : this.story.getChapters()) { // use story chapters because we need merged values
                FormattedText text = chapter.getText();
                if (text != null && this.readChapters.contains(text)) {
                    read.add(text);
                }
            }
            readChapters = read;
        }
        log.trace("saving");
        save();
    }

    public synchronized void setChapterRead(Chapter chapter, boolean read) {
        FormattedText text = chapter.getText();
        if (text == null) { return; }
        boolean changed = read ? readChapters.add(text) : readChapters.remove(text);
        if (changed) {
            save();
        }
    }

    public synchronized boolean isChapterRead(Chapter chapter) {
        return readChapters.contains(chapter.getText());
    }

    private synchronized void save() {
        manager.objectStorage.save(this, getObjectId());
    }

    @JsonIgnore
    public synchronized int getReadChapterCount() {
        return readChapters.size();
    }
}
