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
}
