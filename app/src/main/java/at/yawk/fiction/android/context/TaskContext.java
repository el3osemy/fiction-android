package at.yawk.fiction.android.context;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author yawkat
 */
public class TaskContext {
    private Set<FutureHolder> futures = Collections.newSetFromMap(new ConcurrentHashMap<>());

    FutureHolder add() {
        FutureHolder holder = new FutureHolder();
        futures.add(holder);
        return holder;
    }

    public void destroy() {
        for (FutureHolder future : futures) {
            future.interrupt();
        }
    }

    class FutureHolder {
        private boolean valid = true;
        private Future<?> future;

        synchronized void setFuture(Future<?> future) {
            this.future = future;
            if (!valid) { interrupt(); }
        }

        synchronized void interrupt() {
            this.valid = false;
            if (this.future != null) {
                this.future.cancel(true);
            }
        }

        synchronized void complete() {
            future = null;
            futures.remove(this);
        }
    }
}
