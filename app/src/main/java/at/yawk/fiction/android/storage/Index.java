package at.yawk.fiction.android.storage;

import com.google.common.base.Objects;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;

/**
 * @author yawkat
 */
@Singleton
class Index {
    private Holder holder;

    FileSystemStorage fileSystemStorage;
    StoryManager storyManager; // set by StorageManager

    @Inject
    Index(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
        try {
            holder = fileSystemStorage.load(Holder.class, "index");
        } catch (NotFoundException e) {
            holder = new Holder();
        } catch (UnreadableException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized StoryIndexEntry findIndexEntry(String id) {
        StoryIndexEntry entry = holder.getStories().get(id);
        if (entry == null) {
            StoryWrapper story = storyManager.getStory(id);
            entry = story.createIndexEntry();
            holder.getStories().put(id, entry);
            save();
        }
        return entry;
    }

    synchronized void invalidate(StoryWrapper wrapper) {
        StoryIndexEntry newEntry = wrapper.createIndexEntry();
        StoryIndexEntry oldEntry = holder.getStories().put(storyManager.getObjectId(wrapper.getStory()), newEntry);
        if (!Objects.equal(newEntry, oldEntry)) {
            save();
        }
    }

    private synchronized void save() {
        fileSystemStorage.save(holder, "index");
    }

    @Data
    private static class Holder {
        Map<String, StoryIndexEntry> stories = new HashMap<>();
    }
}
