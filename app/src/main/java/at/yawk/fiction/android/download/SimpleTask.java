package at.yawk.fiction.android.download;

import at.yawk.fiction.android.download.task.DownloadTask;
import at.yawk.fiction.android.download.task.DownloadTaskHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
class SimpleTask<T extends DownloadTask> extends AbstractTask<T> {
    private final DownloadTaskHandler<T> handler;
    private volatile boolean started;
    private volatile boolean complete;
    private volatile boolean cancelled;

    SimpleTask(DownloadManager manager, T task, DownloadTaskHandler<T> handler) {
        super(manager, task);
        this.handler = handler;
    }

    @Override
    protected synchronized void fireCompletion() {
        complete = true;
        super.fireCompletion();
    }

    @Override
    public boolean isRunning() {
        return !complete && started;
    }

    @Override
    public void cancel() {
        cancelled = true;
        fireCompletion();
    }

    @Override
    public void run() {
        if (cancelled) { return; }

        started = true;
        try {
            handler.run(getTask(), getProgressListener());
        } catch (Throwable t) {
            log.error("Failed to execute task", t);
        }
        fireCompletion();
    }
}
