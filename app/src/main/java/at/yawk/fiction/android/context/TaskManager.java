package at.yawk.fiction.android.context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class TaskManager {
    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
        private AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("taskManager-thread-" + index.getAndIncrement());
            return thread;
        }
    });

    @Inject
    TaskManager() {}

    public void execute(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.error("Error in task", t);
            }
        });
    }

    public void execute(TaskContext context, Runnable task) {
        TaskContext.FutureHolder holder = context.add();
        Future<?> future = executor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.error("Error in task", t);
            } finally {
                holder.complete();
            }
        });
        holder.setFuture(future);
    }
}
