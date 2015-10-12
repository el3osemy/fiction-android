package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.ProgressStatus;
import at.yawk.fiction.android.storage.SqlStorage;
import at.yawk.fiction.android.storage.StoryIndexEntry;
import at.yawk.fiction.android.storage.StoryWrapper;
import com.j256.ormlite.stmt.ColumnArg;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * @author yawkat
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocalSearchQuery extends SearchQuery {
    boolean readNone = false;
    boolean readSome = true;
    boolean readAll = true;

    boolean downloadedNone = true;
    boolean downloadedSome = true;
    boolean downloadedAll = true;

    Set<String> excludedProviders = new HashSet<>();

    StoryOrder order = StoryOrder.ALPHABETICAL;

    @SuppressWarnings("unchecked")
    @SneakyThrows(SQLException.class)
    Where<StoryIndexEntry, String> apply(QueryBuilder<StoryIndexEntry, String> builder,
                                         Where<StoryIndexEntry, String> where) {
        order.applyOrder(builder);
        return where.and(
                appendArity(where, "downloadedChapterCount", downloadedNone, downloadedSome, downloadedAll),
                appendArity(where, "readChapterCount", readNone, readSome, readAll),
                where.notIn("providerId", excludedProviders)
        );
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows(SQLException.class)
    private static Where<StoryIndexEntry, String> appendArity(Where<StoryIndexEntry, String> where,
                                                              String progressColumn,
                                                              boolean none, boolean some, boolean all) {

        List<Where<StoryIndexEntry, String>> clauses = new ArrayList<>(3);
        if (none) {
            clauses.add(where.eq(progressColumn, 0));
        }
        if (some) {
            clauses.add(where.and(
                    where.gt(progressColumn, 0),
                    where.lt(progressColumn, new ColumnArg("totalChapterCount"))
            ));
        }
        if (all) {
            clauses.add(where.eq(progressColumn, new ColumnArg("totalChapterCount")));
        }

        return SqlStorage.or(where, clauses);
    }

    boolean accept(StoryWrapper wrapper) {
        if (excludedProviders.contains(wrapper.getProvider().getId())) {
            return false;
        }

        return accept(wrapper.getReadProgressType(), wrapper.getDownloadProgressType());

    }

    private boolean accept(ProgressStatus readStatus, ProgressStatus downloadStatus) {
        switch (readStatus) {
        case NONE:
            if (!readNone) { return false; }
            break;
        case SOME:
            if (!readSome) { return false; }
            break;
        case ALL:
            if (!readAll) { return false; }
            break;
        }

        switch (downloadStatus) {
        case NONE:
            if (!downloadedNone) { return false; }
            break;
        case SOME:
            if (!downloadedSome) { return false; }
            break;
        case ALL:
            if (!downloadedAll) { return false; }
            break;
        }
        return true;
    }
}
