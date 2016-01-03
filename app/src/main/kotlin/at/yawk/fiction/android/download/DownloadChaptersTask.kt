package at.yawk.fiction.android.download

import at.yawk.fiction.android.storage.StoryWrapper
import kotlin.collections.toList

/**
 * @author yawkat
 */
data class DownloadChaptersTask(
        val story: StoryWrapper,
        val chapterIndices: Collection<Int>
) : Task {
    companion object {
        @JvmStatic fun rangeClosed(start: Int, end: Int) = (start..end).toList()
    }
}