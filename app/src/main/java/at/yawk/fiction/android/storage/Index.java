package at.yawk.fiction.android.storage;

import com.j256.ormlite.dao.Dao;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class Index {
    private final SqlStorage sqlStorage;
    final Dao<StoryIndexEntry, String> indexDao;

    StoryManager storyManager; // set by StoryManager

    @Inject
    @SneakyThrows(SQLException.class)
    Index(SqlStorage sqlStorage) {
        this.sqlStorage = sqlStorage;
        indexDao = sqlStorage.createDao(StoryIndexEntry.class);
    }

    void initStoryManager() {
        if (sqlStorage.getMigrationCount() > 0) {
            buildIndex();
        }
    }

    @SneakyThrows(SQLException.class)
    public void buildIndex() {
        indexDao.updateRaw("DELETE FROM storyIndex");
        for (StoryWrapper wrapper : storyManager.listStories()) {
            indexDao.createOrUpdate(wrapper.createIndexEntry());
        }
    }

    @SneakyThrows(SQLException.class)
    synchronized StoryIndexEntry findIndexEntry(String id) {
        StoryIndexEntry entry = indexDao.queryForId(id);
        if (entry == null) {
            StoryWrapper story = storyManager.getStory(id);
            entry = story.createIndexEntry();
            indexDao.create(entry);
        }
        return entry;
    }

    @SneakyThrows(SQLException.class)
    synchronized void invalidate(StoryWrapper wrapper) {
        StoryIndexEntry newEntry = wrapper.createIndexEntry();
        indexDao.createOrUpdate(newEntry);
    }
}
