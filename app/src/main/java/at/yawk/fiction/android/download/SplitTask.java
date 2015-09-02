package at.yawk.fiction.android.download;

import at.yawk.fiction.android.download.task.DownloadTask;
import at.yawk.fiction.android.download.task.SplittableDownloadTask;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yawkat
 */
class SplitTask extends AbstractTask<SplittableDownloadTask> {
    private final List<AbstractTask> subTasks;
    private final int taskCount;
    private final AtomicInteger completed = new AtomicInteger(0);

    private volatile boolean cancelled = false;

    public SplitTask(DownloadManager manager, SplittableDownloadTask task) {
        super(manager, task);

        List<DownloadTask> tasks = task.getTasks();
        taskCount = tasks.size();

        this.subTasks = new ArrayList<>(taskCount);
        for (DownloadTask subTask : tasks) {
            AbstractTask wrapper = manager.createTask(subTask);
            this.subTasks.add(wrapper);
            wrapper.addCompletionListener(() -> {
                int completed = this.completed.incrementAndGet();
                getProgressListener().progressDeterminate(completed, taskCount);
                if (completed == taskCount) {
                    fireCompletion();
                }
            });
        }
    }

    @Override
    public boolean isRunning() {
        return completed.get() < taskCount && !cancelled;
    }

    @Override
    public void cancel() {
        if (cancelled) { return; }

        // reverse direction so no new tasks get enqueued
        ListIterator<AbstractTask> iterator = subTasks.listIterator(subTasks.size());
        while (iterator.hasPrevious()) {
            iterator.previous().cancel();
        }
        cancelled = true;
    }

    @Override
    public void run() {
        if (cancelled) { return; }
        for (AbstractTask subTask : subTasks) {
            subTask.schedule();
        }
    }
}
