package at.yawk.fiction.android.download;

import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.event.EventBus;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Singleton
@Slf4j
public class DownloadManager implements DownloadManagerMetrics {
    private final TaskContext context = new TaskContext();
    private final Map<Class<? extends DownloadTask>, DownloadTaskHandler<?>> handlers = new HashMap<>();

    @Inject TaskManager taskManager;
    @Inject EventBus bus;

    /**
     * All currently queued tasks, including the currently running task.
     */
    private final Queue<Runner<?>> runnerQueue = new ArrayDeque<>();

    @Inject
    DownloadManager(
            ChapterDownloadTaskHandler chapterDownloadTaskHandler
    ) {
        addHandler(ChapterDownloadTask.class, chapterDownloadTaskHandler);
    }

    private <T extends DownloadTask> void addHandler(Class<T> type, DownloadTaskHandler<T> handler) {
        handlers.put(type, handler);
    }

    public void enqueue(DownloadTask task) {
        Runner<DownloadTask> runner = new Runner<>(MultiTaskExecutionStrategy.SEQUENTIAL, task);
        runner.progressListener = runner;
        runner.addCompletionCallback(() -> {
            synchronized (this) {
                Runner<?> completed = runnerQueue.poll();
                assert completed == runner;
                Runner<?> next = runnerQueue.peek();
                if (next != null) {
                    next.scheduleOrSplit();
                }
            }
            postManagerUpdateEvent();
        });
        synchronized (this) {
            boolean startNow = runnerQueue.isEmpty();
            runnerQueue.offer(runner);
            if (startNow) {
                runner.scheduleOrSplit();
            }
        }
        postManagerUpdateEvent();
    }

    private void postManagerUpdateEvent() {
        bus.post(new ManagerUpdateEvent());
    }

    @Override
    public synchronized List<Task> getTasks() {
        return new ArrayList<>(runnerQueue);
    }

    @RequiredArgsConstructor
    private class Runner<T extends DownloadTask> implements Runnable, Task, ProgressListener {
        final MultiTaskExecutionStrategy multiTaskExecutionStrategy;
        final T task;
        ProgressListener progressListener = ProgressListener.NOOP;

        volatile long currentProgress = 0;
        volatile long maxProgress = -1;

        @Nullable private Runnable completionCallback;

        volatile boolean running = false;

        void scheduleOrSplit() {
            running = true;
            postUpdateEvent();
            if (task instanceof SplittableDownloadTask) {
                runSplit(((SplittableDownloadTask) task).getTasks());
            } else {
                taskManager.execute(context, this);
            }
        }

        private void runSplit(List<DownloadTask> tasks) {
            List<Runnable> runners = new ArrayList<>(tasks.size());
            AtomicLong completed = new AtomicLong(0);
            for (DownloadTask subTask : tasks) {
                Runner<?> runner = new Runner<>(multiTaskExecutionStrategy, subTask);
                runner.progressListener = progressListener.createSubLevel();
                runner.addCompletionCallback(
                        () -> progressListener.progressDeterminate(completed.incrementAndGet(), tasks.size()));
                runners.add(runner);
            }

            progressListener.progressDeterminate(0, tasks.size());
            multiTaskExecutionStrategy.execute(r -> taskManager.execute(context, r),
                                               runners,
                                               this::complete);
        }

        private void complete() {
            running = false;
            postUpdateEvent();
            if (completionCallback != null) {
                completionCallback.run();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            try {
                ((DownloadTaskHandler<T>) handlers.get(task.getClass())).run(task, progressListener);
            } catch (Throwable e) {
                log.error("Failed to run task", e);
                // todo
            }
            complete();
        }

        void addCompletionCallback(@Nullable Runnable callback) {
            if (callback == null) { return; }
            if (completionCallback == null) {
                completionCallback = callback;
            } else {
                Runnable old = completionCallback;
                completionCallback = () -> {
                    old.run();
                    callback.run();
                };
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public String getName() {
            return task.getName();
        }

        @Override
        public long getCurrentProgress() {
            return currentProgress;
        }

        @Override
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
            postUpdateEvent();
        }

        @Override
        public void progressIndeterminate(boolean complete) {
            progressDeterminate(0, -1);
        }

        private void postUpdateEvent() {
            bus.post(new TaskUpdateEvent(this));
        }
    }
}
