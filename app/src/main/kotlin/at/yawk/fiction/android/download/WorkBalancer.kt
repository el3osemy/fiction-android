package at.yawk.fiction.android.download

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore

/**
 * @author yawkat
 */
interface WorkBalancer {
    fun execute(group: TaskGroup, task: Runnable)

    fun <V> execute(group: TaskGroup, task: Callable<out V>, reportErrors: Boolean = true): ListenableFuture<in V>
}

interface TaskGroup {
    val cancelled: Boolean
    fun report(e: Throwable)
}

internal abstract class AbstractWorkBalancer : WorkBalancer {
    override fun execute(group: TaskGroup, task: Runnable) {
        executeSafe(Runnable {
            if (group.cancelled) return@Runnable
            try {
                task.run()
            } catch(e: Throwable) {
                group.report(e)
            }
        })
    }

    override fun <V> execute(group: TaskGroup, task: Callable<out V>, reportErrors: Boolean): ListenableFuture<in V> {
        val future = SettableFuture.create<V>()
        executeSafe(Runnable {
            if (group.cancelled) {
                future.cancel(false)
                return@Runnable
            }
            try {
                future.set(task.call())
            } catch(e: Throwable) {
                if (reportErrors) group.report(e)
                future.setException(e)
            }
        })
        return future
    }

    abstract fun executeSafe(task: Runnable)
}

internal class SemaphoreWorkBalancer(
        val executor: Executor,
        val semaphore: Semaphore
) : AbstractWorkBalancer() {
    private val workQueue = ArrayDeque<Runnable>()

    override fun executeSafe(task: Runnable) {
        synchronized(this) { workQueue.offer(task) }
        checkForWork()
    }

    private fun checkForWork() {
        if (semaphore.tryAcquire()) {
            val task = synchronized(this) { workQueue.poll() }
            if (task == null) {
                semaphore.release()
            } else {
                executor.execute {
                    try {
                        task.run()
                    } finally {
                        semaphore.release()
                        checkForWork()
                    }
                }
            }
        }
    }
}