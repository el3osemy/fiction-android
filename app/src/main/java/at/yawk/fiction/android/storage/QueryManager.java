package at.yawk.fiction.android.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * @author yawkat
 */
public class QueryManager {
    private transient ObjectStorageManager objectStorageManager;
    @JsonProperty private List<QueryWrapper> queries;
    @JsonProperty private UUID selectedQueryId;

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
        save();
    }

    public void removeQuery(QueryWrapper wrapper) {
        for (Iterator<QueryWrapper> iterator = queries.iterator(); iterator.hasNext(); ) {
            if (iterator.next().getId().equals(wrapper.getId())) {
                iterator.remove();
            }
        }
        save();
    }

    public UUID getSelectedQueryId() {
        return selectedQueryId;
    }

    @JsonIgnore
    public void setSelectedQueryId(UUID selectedQueryId) {
        this.selectedQueryId = selectedQueryId;
        save();
    }

    public List<QueryWrapper> getQueries() {
        return queries;
    }

    private void save() {
        objectStorageManager.save(this, "queryManager");
    }

    static QueryManager load(ObjectStorageManager objectStorageManager) {
        QueryManager manager;
        try {
            manager = objectStorageManager.load(QueryManager.class, "queryManager");
        } catch (NotFoundException e) {
            manager = new QueryManager();
            manager.queries = new ArrayList<>();
        }
        manager.objectStorageManager = objectStorageManager;
        return manager;
    }
}
