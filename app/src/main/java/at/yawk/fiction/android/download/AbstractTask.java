package at.yawk.fiction.android.download;

import at.yawk.fiction.android.download.task.DownloadTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/**
 * @author yawkat
 */
abstract class AbstractTask<T extends DownloadTask> implements DownloadManagerMetrics.Task, Runnable {
    @Getter private final DownloadManager manager;
    @Getter private final T task;
    private final SimpleTaskProgressHolder progressHolder;

    private Object completionListeners = null;

    AbstractTask(DownloadManager manager, T task) {
        this.manager = manager;
        this.task = task;
        this.progressHolder = new SimpleTaskProgressHolder(manager.bus, this);
    }

    @SuppressWarnings("unchecked")
    public synchronized void addCompletionListener(Runnable listener) {
        if (completionListeners == null) {
            completionListeners = listener;
        } else if (completionListeners instanceof Runnable) {
            completionListeners = new ArrayList<>(Arrays.asList(this.completionListeners, listener));
        } else {
            ((ArrayList<Runnable>) completionListeners).add(listener);
        }
    }

    @SuppressWarnings("unchecked")
    protected synchronized void fireCompletion() {
        if (completionListeners != null) {
            if (completionListeners instanceof Runnable) {
                ((Runnable) completionListeners).run();
            } else {
                for (Runnable listener : (List<Runnable>) completionListeners) {
                    listener.run();
                }
            }
        }
    }

    protected ProgressListener getProgressListener() {
        return progressHolder;
    }

    @Override
    public String getName() {
        return task.getName();
    }

    @Override
    public long getCurrentProgress() {
        return progressHolder.getCurrentProgress();
    }

    @Override
    public long getMaxProgress() {
        return progressHolder.getMaxProgress();
    }

    public final void schedule() {
        manager.getExecutor().execute(this);
    }
}
