package at.yawk.fiction.android.download

import at.yawk.fiction.android.storage.QueryWrapper

/**
 * @author yawkat
 */
data class DownloadQueryPagesTask(
        val query: QueryWrapper
) : Task