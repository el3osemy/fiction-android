package at.yawk.fiction.android.storage;

import at.yawk.fiction.SearchQuery;
import lombok.Data;

/**
 * @author yawkat
 */
@Data
public class QueryWrapper {
    private String name;
    private SearchQuery query;
}
