package at.yawk.fiction.android.download;

import at.yawk.fiction.android.download.task.TaskUpdateEvent;
import at.yawk.fiction.android.event.EventBus;

/**
 * @author yawkat
 */
class SimpleTaskProgressHolder implements ProgressListener {
    private final EventBus bus;
    // flyweight update event
    private final TaskUpdateEvent event;

    volatile long currentProgress = 0;
    volatile long maxProgress = -1;

    SimpleTaskProgressHolder(EventBus bus, DownloadManagerMetrics.Task task) {
        this.bus = bus;
        this.event = new TaskUpdateEvent(task);
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public long getMaxProgress() {
        return maxProgress;
    }

    @Override
    public ProgressListener createSubLevel() {
        return ProgressListener.NOOP;
    }

    @Override
    public void progressDeterminate(long progress, long limit) {
        currentProgress = progress;
        maxProgress = limit;
        bus.post(event);
    }

    @Override
    public void progressIndeterminate(boolean complete) {
        progressDeterminate(0, -1);
    }
}
