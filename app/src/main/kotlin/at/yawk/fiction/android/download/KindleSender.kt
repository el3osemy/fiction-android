package at.yawk.fiction.android.download

import android.app.Application
import at.yawk.fiction.android.storage.EpubBuilder
import at.yawk.fiction.android.storage.StoryWrapper
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
class KindleSender @Inject constructor(application: Application, objectMapper: ObjectMapper,
                                       val epubBuilder: EpubBuilder) {
    val config = objectMapper.readValue(application.assets.open("kindle_send.json"), Config::class.java)
    val enabled = config.enabled

    fun send(storyWrapper: StoryWrapper) {
        val connection = URL(config.url).openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.doInput = true
        connection.useCaches = false
        connection.addRequestProperty("X-Api-Token", config.apiKey)
        epubBuilder.buildEpub(storyWrapper, connection.outputStream)
        connection.inputStream.readBytes()
    }

    data class Config(
            val enabled: Boolean,
            val url: String,
            val apiKey: String
    )
}