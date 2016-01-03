package at.yawk.fiction.android.download

import at.yawk.fiction.android.storage.StoryWrapper

/**
 * @author yawkat
 */
data class UpdateStoryListTask(
        val stories: List<StoryWrapper>
) : Task