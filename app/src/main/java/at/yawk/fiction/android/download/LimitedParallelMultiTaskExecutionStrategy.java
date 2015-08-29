package at.yawk.fiction.android.download;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;

/**
 * {@link MultiTaskExecutionStrategy} that only supports a set amount of parallel threads but supports task
 * cancellation.
 *
 * @author yawkat
 */
@RequiredArgsConstructor
public class LimitedParallelMultiTaskExecutionStrategy implements MultiTaskExecutionStrategy {
    private final int limit;

    @Override
    public void execute(Executor executor, List<Runnable> tasks, Runnable completionCallback,
                        CancelProvider cancelProvider) {
        Iterator<Runnable> iterator = tasks.iterator();
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable wrappedCompletionCallback = () -> {
            if (completed.compareAndSet(false, true)) {
                completionCallback.run();
            }
        };
        executeSome(executor, new Semaphore(limit), iterator, wrappedCompletionCallback, cancelProvider);
    }

    private static void executeSome(Executor executor,
                                   Semaphore semaphore,
                                   Iterator<Runnable> taskIterator,
                                   Runnable completionCallback,
                                   CancelProvider cancelProvider) {
        if (cancelProvider.isCancelled()) {
            completionCallback.run();
            return;
        }

        while (semaphore.tryAcquire()) {
            Runnable task;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (taskIterator) {
                if (taskIterator.hasNext()) {
                    task = taskIterator.next();
                } else {
                    completionCallback.run();
                    return;
                }
            }
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    semaphore.release();
                    executeSome(executor, semaphore, taskIterator, completionCallback, cancelProvider);
                }
            });
        }
    }
}
