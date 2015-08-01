package at.yawk.fiction.android.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;

/**
 * @author yawkat
 */
@Singleton
public class QueryManager {
    @Inject ObjectStorageManager objectStorageManager;
    private Holder holder;

    @Inject
    void load() {
        try {
            holder = objectStorageManager.load(Holder.class, "queryManager");
        } catch (NotFoundException e) {
            holder = new Holder();
            holder.setQueries(new ArrayList<>());
            holder.setSelectedQueryId(null);
        }
    }

    private synchronized void save() {
        objectStorageManager.save(holder, "queryManager");
    }

    public synchronized void saveQuery(QueryWrapper wrapper) {
        boolean found = false;
        for (int i = 0; i < holder.queries.size(); i++) {
            if (holder.queries.get(i).getId().equals(wrapper.getId())) {
                holder.queries.set(i, wrapper);
                found = true;
                break;
            }
        }
        if (!found) { holder.queries.add(wrapper); }
        save();
    }

    public synchronized void removeQuery(QueryWrapper wrapper) {
        for (Iterator<QueryWrapper> iterator = holder.queries.iterator(); iterator.hasNext(); ) {
            if (iterator.next().getId().equals(wrapper.getId())) {
                iterator.remove();
            }
        }
        save();
    }

    public UUID getSelectedQueryId() {
        return holder.selectedQueryId;
    }

    public synchronized void setSelectedQueryId(UUID selectedQueryId) {
        holder.selectedQueryId = selectedQueryId;
        save();
    }

    public List<QueryWrapper> getQueries() {
        return holder.queries;
    }

    public void moveQuery(int from, int to) {
        QueryWrapper obj = holder.queries.remove(from);
        holder.queries.add(to, obj);
        save();
    }

    @Data
    private static class Holder {
        List<QueryWrapper> queries;
        UUID selectedQueryId;
    }
}
