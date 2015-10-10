package at.yawk.fiction.android.storage;

import at.yawk.fiction.Pageable;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;

/**
 * @author yawkat
 */

public class OfflineQueryManager {
    @Inject ObjectStorageManager objectStorageManager;
    @Inject StorageManager storageManager;

    public void save(QueryWrapper query, int index, Pageable.Page<StoryWrapper> entries) {
        String key = query.getId() + "/" + index;

        OfflinePage offlinePage = new OfflinePage();
        offlinePage.setStoryIds(Lists.transform(entries.getEntries(), StoryWrapper::getId));
        objectStorageManager.save(offlinePage, key);
    }

    public Pageable<StoryWrapper> load(QueryWrapper query) {
        return i -> {
            String key = query.getId() + "/" + i;
            try {
                OfflinePage offlinePage = objectStorageManager.load(OfflinePage.class, key);

                Pageable.Page<StoryWrapper> page = new Pageable.Page<>();
                page.setEntries(Lists.transform(offlinePage.getStoryIds(), storageManager::getStory));
                page.setLast(false);
                return page;
            } catch (NotFoundException notFound) {
                Pageable.Page<StoryWrapper> page = new Pageable.Page<>();
                page.setEntries(Collections.emptyList());
                page.setPageCount(i);
                page.setLast(true);
                return page;
            }
        };
    }

    @Data
    private static class OfflinePage {
        List<String> storyIds;
    }
}
