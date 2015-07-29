package at.yawk.fiction.android.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yawkat
 */
public class QueryManager {
    private transient ObjectStorageManager objectStorageManager;
    @Getter @Setter private List<QueryWrapper> queries;
    @Getter @Setter private UUID selectedQueryId;

    public void saveQuery(QueryWrapper wrapper) {
        boolean found = false;
        for (int i = 0; i < queries.size(); i++) {
            if (queries.get(i).getId().equals(wrapper.getId())) {
                queries.set(i, wrapper);
                found = true;
                break;
            }
        }
        if (!found) { queries.add(wrapper); }
        objectStorageManager.save(this, "queryManager");
    }

    static QueryManager load(ObjectStorageManager objectStorageManager) {
        QueryManager manager;
        try {
            manager = objectStorageManager.load(QueryManager.class, "queryManager");
        } catch (NotFoundException e) {
            manager = new QueryManager();
            manager.setQueries(new ArrayList<>());
        }
        manager.objectStorageManager = objectStorageManager;
        return manager;
    }
}
