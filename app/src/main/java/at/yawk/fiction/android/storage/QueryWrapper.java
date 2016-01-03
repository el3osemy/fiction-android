package at.yawk.fiction.android.storage;

import at.yawk.fiction.SearchQuery;
import java.util.UUID;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class QueryWrapper {
    private UUID id;
    private String name;
    private SearchQuery query;

    // MANUAL GETTERS FOR KOTLIN

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SearchQuery getQuery() {
        return query;
    }
}
