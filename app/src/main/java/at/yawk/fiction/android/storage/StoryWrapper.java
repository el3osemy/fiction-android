package at.yawk.fiction.android.storage;

import at.yawk.fiction.Story;

/**
 * @author yawkat
 */
public class StoryWrapper {
    transient String objectId;
    transient StorageManager manager;
    Story story;

    void setStory(Story story) {
        this.story = story;
    }

    public Story getStory() {
        return story;
    }

    String getObjectId() {
        if (objectId == null) {
            objectId = manager.getObjectId(story);
        }
        return objectId;
    }

    public synchronized void updateStory(Story changes) {
        if (!changes.equals(story)) { // story can be null
            story = story == null ? changes : manager.pojoMerger.merge(changes, story);
            manager.objectStorage.save(this, getObjectId());
        }
    }
}
