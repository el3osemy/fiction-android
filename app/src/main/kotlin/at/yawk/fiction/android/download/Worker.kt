package at.yawk.fiction.android.download

import at.yawk.fiction.Story
import at.yawk.fiction.android.context.TaskManager
import at.yawk.fiction.android.provider.ProviderManager
import at.yawk.fiction.android.storage.OfflineQueryManager
import at.yawk.fiction.android.storage.PojoMerger
import at.yawk.fiction.android.storage.StoryWrapper
import java.util.concurrent.Executor
import java.util.concurrent.Semaphore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
internal class Worker @Inject constructor(
        val pojoMerger: PojoMerger,
        val taskManager: TaskManager,
        val providerManager: ProviderManager,
        val offlineQueryManager: OfflineQueryManager,
        val kindleSender: KindleSender
) {
    private val workBalancer: WorkBalancer = SemaphoreWorkBalancer(
            Executor { taskManager.execute(it) },
            Semaphore(4)
    )

    fun getTaskName(task: Task): String {
        when (task) {
            is DownloadChaptersTask -> {
                if (task.chapterIndices.size == 1) {
                    return "Downloading chapter ${task.chapterIndices.first()} of '${getStoryName(task.story)}'"
                }
                return "Downloading ${task.chapterIndices.size} chapters of '${getStoryName(task.story)}'"
            }
            is DownloadQueryPagesTask -> return "Fetching query '${task.query.name}'"
            is UpdateStoryListTask -> {
                if (task.stories.size == 1) {
                    return "Updating story '${getStoryName(task.stories[0])}'"
                }
                return "Updating ${task.stories.size} stories"
            }
            is SendBookToKindleTask -> return "Sending '${getStoryName(task.story)}' to kindle"
            else -> throw UnsupportedOperationException()
        }
    }

    fun execute(task: Task, listener: ProgressListener, group: TaskGroup) {
        when (task) {
            is DownloadChaptersTask -> downloadChapters(task, listener, group)
            is DownloadQueryPagesTask -> downloadQueryPages(task, listener, group)
            is UpdateStoryListTask -> updateStoryList(task, listener, group)
            is SendBookToKindleTask -> workBalancer.execute(group, Runnable {
                listener.postProgress(ProgressListener.Indeterminate("Sending"))
                try {
                    kindleSender.send(task.story)
                } finally {
                    listener.postProgress(ProgressListener.Complete)
                }
            })
            else -> throw UnsupportedOperationException()
        }
    }

    private fun downloadChapters(task: DownloadChaptersTask, listener: ProgressListener, group: TaskGroup) {
        val progressor = Progressor(listener, 0, task.chapterIndices.size)
        progressor.publish("Downloading ${task.chapterIndices.size} chapters")
        for (chapterIndex in task.chapterIndices) {
            workBalancer.execute(group, Runnable {
                try {
                    task.story.setDownloading(chapterIndex, true)
                    try {
                        val storyClone = pojoMerger.clone(task.story.story)
                        val chapter = storyClone.chapters[chapterIndex]
                        task.story.provider.fetchChapter(storyClone, chapter)
                        task.story.updateStory(storyClone)
                    } finally {
                        task.story.setDownloading(chapterIndex, false)
                    }
                } finally {
                    progressor.increment("Downloaded chapter $chapterIndex")
                }
            })
        }
    }

    private fun downloadQueryPages(task: DownloadQueryPagesTask, listener: ProgressListener, group: TaskGroup) {
        listener.postProgress(ProgressListener.Indeterminate("Loading first page"))

        val provider = providerManager.getProvider(task.query.query)
        val pageable = provider.searchWrappers(task.query.query)
        workBalancer.execute(group, Runnable {
            val firstPage = pageable.getPage(0)
            offlineQueryManager.save(task.query, 0, firstPage)
            val pageCount = firstPage.pageCount
            if (pageCount < 0) {
                // page count unknown
                listener.postProgress(ProgressListener.Indeterminate("Loaded page #0"))
                var i = 1
                while (true) {
                    if (group.cancelled) break
                    try {
                        val page = pageable.getPage(i)
                        offlineQueryManager.save(task.query, i, page)
                        listener.postProgress(ProgressListener.Indeterminate("Loaded page #$i"))
                        if (page.isLast) break
                    } catch(e: Throwable) {
                        group.report(e)
                        listener.postProgress(ProgressListener.Indeterminate("Failed to load page #$i"))
                    }
                    i++
                }
                listener.postProgress(ProgressListener.Complete)
            } else {
                val progressor = Progressor(listener, 1, pageCount)
                for (i in 1..pageCount - 1) {
                    workBalancer.execute(group, Runnable {
                        val page = pageable.getPage(i)
                        offlineQueryManager.save(task.query, i, page)
                        progressor.increment("Loaded page #$i")
                    })
                }
            }
        })
    }

    private fun updateStoryList(task: UpdateStoryListTask, listener: ProgressListener, group: TaskGroup) {
        val progressor = Progressor(listener, 0, task.stories.size)
        for (story in task.stories) {
            workBalancer.execute(group, Runnable {
                try {
                    val storyClone = pojoMerger.clone<Story>(story.story)
                    val provider = story.provider
                    provider.fetchStory(storyClone)
                    story.updateStory(storyClone)
                } finally {
                    progressor.increment("Updated '${getStoryName(story)}'")
                }
            })
        }
    }

    private fun getStoryName(story: StoryWrapper) = story.story.title ?: story.id

    private class Progressor(
            val listener: ProgressListener,
            @Volatile var complete: Int,
            val total: Int
    ) {
        fun publish(message: String) {
            if (complete >= total) {
                listener.postProgress(ProgressListener.Complete)
            } else if (total == 1) {
                listener.postProgress(ProgressListener.Indeterminate(message))
            } else {
                listener.postProgress(ProgressListener.Determinate(message, complete, total))
            }
        }

        fun increment(message: String) {
            synchronized(this) {
                complete++
                publish(message)
            }
        }
    }
}