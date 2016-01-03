package at.yawk.fiction.android.download

/**
 * @author yawkat
 */
interface ProgressListener {
    fun postProgress(progress: Progress)

    interface Progress

    data class Determinate(
            val message: String,
            val complete: Int,
            val total: Int
    ) : Progress

    data class Indeterminate(val message: String) : Progress

    object Complete : Progress
}