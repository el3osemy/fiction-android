package at.yawk.fiction.android.download

import at.yawk.fiction.android.download.task.TaskUpdateEvent
import at.yawk.fiction.android.event.EventBus
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
class DownloadManagerImpl @Inject internal constructor(
        private val worker: Worker,
        private val eventBus: EventBus
) : DownloadManagerMetrics, DownloadManager {
    private val tasks = CopyOnWriteArraySet<TaskHolder>()

    override fun enqueue(task: Task) {
        val holder = TaskHolder(task)
        tasks.add(holder)
        fireManagerUpdateEvent()
        worker.execute(task, holder, holder)
    }

    override fun getTasks(): Collection<DownloadManagerMetrics.Task> {
        return Collections.unmodifiableCollection(tasks)
    }

    private fun fireManagerUpdateEvent() {
        eventBus.post(ManagerUpdateEvent())
    }

    companion object {
        val log = LoggerFactory.getLogger(DownloadManagerImpl::class.java)
    }

    private inner class TaskHolder(task: Task) : DownloadManagerMetrics.Task, ProgressListener, TaskGroup {
        val name = worker.getTaskName(task)
            @JvmName("getName0") get
        override var cancelled = false
        @Volatile var complete = false
        var statusMessage: String? = null
            @JvmName("getStatusMessage0") get

        var currentProgress = 0
        var maxProgress = -1

        override fun getName() = name

        override fun getStatusMessage() = statusMessage

        override fun isRunning(): Boolean {
            return !cancelled && !complete
        }

        override fun getCurrentProgress(): Long = currentProgress.toLong()

        override fun getMaxProgress(): Long = maxProgress.toLong()

        override fun cancel() {
            complete(true)
        }

        override fun postProgress(progress: ProgressListener.Progress) {
            when (progress) {
                is ProgressListener.Determinate -> {
                    currentProgress = progress.complete
                    maxProgress = progress.total
                    statusMessage = progress.message
                    fireTaskUpdateEvent()
                }
                is ProgressListener.Indeterminate -> {
                    currentProgress = 0
                    maxProgress = -1
                    statusMessage = progress.message
                    fireTaskUpdateEvent()
                }
                is ProgressListener.Complete -> complete(false)
                else -> throw UnsupportedOperationException()
            }
        }

        private fun complete(cancel: Boolean) {
            synchronized(this) {
                if (!complete) {
                    if (cancel) cancelled = true
                    complete = true
                    tasks.remove(this)
                    fireManagerUpdateEvent()
                    fireTaskUpdateEvent()
                }
            }
        }

        fun fireTaskUpdateEvent() {
            eventBus.post(TaskUpdateEvent(this))
        }

        override fun report(e: Throwable) {
            log.error("Error in task '{}'", name, e)
            // todo: toast
        }
    }
}