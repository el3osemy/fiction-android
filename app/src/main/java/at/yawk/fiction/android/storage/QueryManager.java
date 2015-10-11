package at.yawk.fiction.android.storage;

import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.event.QueryListUpdateEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Singleton
@Slf4j
public class QueryManager {
    private final FileSystemStorage fileSystemStorage;
    private Holder holder;

    @Inject EventBus bus;

    @Inject
    public QueryManager(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
        try {
            holder = fileSystemStorage.load(Holder.class, "queryManager");
        } catch (NotFoundException e) {
            holder = new Holder();
            holder.setQueries(new ArrayList<>());
            holder.setSelectedQueryId(null);
        } catch (UnreadableException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void save() {
        fileSystemStorage.save(holder, "queryManager");
        bus.post(new QueryListUpdateEvent());
    }

    public synchronized void saveQuery(QueryWrapper wrapper) {
        boolean found = false;
        for (int i = 0; i < holder.queries.size(); i++) {
            QueryWrapper here = holder.queries.get(i);
            if (here.getId().equals(wrapper.getId())) {
                log.trace("found {} -> {}", here, wrapper);
                if (here != wrapper) {
                    here.setName(wrapper.getName());
                    here.setQuery(wrapper.getQuery());
                }
                found = true;
                break;
            }
        }
        if (!found) {
            log.trace("not found, append instead");
            holder.queries.add(wrapper);
        }
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
