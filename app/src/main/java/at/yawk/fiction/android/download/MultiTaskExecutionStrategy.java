package at.yawk.fiction.android.download;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author yawkat
 */
public interface MultiTaskExecutionStrategy {
    MultiTaskExecutionStrategy SEQUENTIAL = new MultiTaskExecutionStrategy() {
        @Override
        public void execute(Executor executor, List<Runnable> tasks, Runnable completionCallback,
                            CancelProvider cancelProvider) {
            execute(executor, tasks.iterator(), completionCallback, cancelProvider);
        }

        private void execute(Executor executor, Iterator<Runnable> iterator, Runnable completionCallback,
                             CancelProvider cancelProvider) {
            if (cancelProvider.isCancelled() || !iterator.hasNext()) {
                completionCallback.run();
            } else {
                executor.execute(() -> {
                    try {
                        iterator.next().run();
                    } finally {
                        execute(executor, iterator, completionCallback, cancelProvider);
                    }
                });
            }
        }
    };
    MultiTaskExecutionStrategy PARALLEL = (executor, tasks, completionCallback, cancelProvider) -> {
        AtomicLong running = new AtomicLong(tasks.size());
        for (Runnable task : tasks) {
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    if (running.decrementAndGet() == 0) {
                        completionCallback.run();
                    }
                }
            });
        }
    };

    void execute(Executor executor, List<Runnable> tasks, Runnable completionCallback, CancelProvider cancelProvider);

    interface CancelProvider {
        boolean isCancelled();
    }
}
