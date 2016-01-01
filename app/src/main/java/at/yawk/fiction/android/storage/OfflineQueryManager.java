package at.yawk.fiction.android.storage;

import at.yawk.fiction.Pageable;
import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class OfflineQueryManager {
    @Inject FileSystemStorage fileSystemStorage;
    @Inject StoryManager storyManager;

    private final Dao<OfflineQueryEntry, Object> dao;

    @Inject
    @SneakyThrows(SQLException.class)
    OfflineQueryManager(SqlStorage sqlStorage) {
        dao = sqlStorage.createDao(OfflineQueryEntry.class);
    }

    @SneakyThrows(SQLException.class)
    public void save(QueryWrapper query, int index, Pageable.Page<StoryWrapper> entries) {
        String queryId = query.getId().toString();
        dao.executeRaw("delete from offlineQuery where queryId=? and pageIndex=?", queryId, String.valueOf(index));
        for (int i = 0; i < entries.getEntries().size(); i++) {
            StoryWrapper wrapper = entries.getEntries().get(i);
            OfflineQueryEntry entry = new OfflineQueryEntry();
            entry.setQueryId(queryId);
            entry.setPageIndex(index);
            entry.setItemIndex(i);
            entry.setStoryId(wrapper.getId());
            dao.create(entry);
        }
    }

    @SneakyThrows(SQLException.class)
    public Pageable<StoryWrapper> load(QueryWrapper query) {
        String maxText = dao.queryRaw("select max(pageIndex) from offlineQuery where queryId=?",
                                      query.getId().toString()).getFirstResult()[0];
        int maxPage = maxText == null ? 0 : Integer.parseInt(maxText);
        return i -> {
            List<StoryWrapper> items;
            if (maxPage >= i) {
                QueryBuilder<OfflineQueryEntry, Object> builder = dao.queryBuilder();
                builder.where().eq("queryId", query.getId().toString())
                        .and().eq("pageIndex", i);
                builder.orderBy("itemIndex", true);
                items = new ArrayList<>(Lists.transform(
                        builder.query(), offlineQueryEntry -> storyManager.getStory(offlineQueryEntry.storyId)));
            } else {
                items = Collections.emptyList();
            }

            Pageable.Page<StoryWrapper> page = new Pageable.Page<>();
            page.setEntries(items);
            page.setLast(i >= maxPage);
            page.setPageCount(maxPage + 1);
            return page;
        };
    }

    @Data
    @DatabaseTable(tableName = "offlineQuery")
    public static class OfflineQueryEntry {
        @DatabaseField(canBeNull = false)
        private String queryId;
        @DatabaseField(canBeNull = false)
        private int pageIndex;
        @DatabaseField(canBeNull = false)
        private int itemIndex;
        @DatabaseField(canBeNull = false)
        private String storyId;
    }
}
