package at.yawk.fiction.android.context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class TaskManager {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void execute(TaskContext context, Runnable task) {
        TaskContext.FutureHolder holder = context.add();
        Future<?> future = executor.submit(() -> {
            log.info("exec");
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
