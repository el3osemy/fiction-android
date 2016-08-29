package at.yawk.fiction.android.provider.ao3

import at.yawk.fiction.Story
import at.yawk.fiction.android.provider.AndroidFictionProvider
import at.yawk.fiction.android.provider.Provider
import at.yawk.fiction.impl.PageParserProvider
import at.yawk.fiction.impl.ao3.*
import com.fasterxml.jackson.databind.ObjectMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author yawkat
 */
@Singleton
@Provider(priority = 3000)
class Ao3AndroidFictionProvider : AndroidFictionProvider(
        "ao3", "Archive Of Our Own",
        Ao3Story::class.java, Ao3Chapter::class.java, Ao3Author::class.java, Ao3SearchQuery::class.java) {

    @Inject lateinit internal var pageParserProvider: PageParserProvider

    private val _fictionProvider: Ao3FictionProvider by lazy {
        Ao3FictionProvider(pageParserProvider, createHttpClient(), ObjectMapper())
    }

    override public fun getFictionProvider() = _fictionProvider

    override fun createQueryEditorFragment() = Ao3QueryEditorFragment()

    override fun getStoryId(story: Story, separator: String) = (story as Ao3Story).id.toString()

    override fun getTags(story: Story): List<String> {
        if (story !is Ao3Story) throw UnsupportedOperationException()
        return (story.warnings ?: emptyList()) +
                (story.requiredTags ?: emptyList()) +
                (story.relationships ?: emptyList()) +
                (story.characters ?: emptyList()) +
                (story.freeforms ?: emptyList()) +
                "Words: ${story.words}" +
                "Comments: ${story.commentCount}" +
                "Kudos: ${story.kudoCount}" +
                "Hits: ${story.hitCount}" +
                "Chapter Goal: ${story.chapterGoal ?: "?"}"
    }
}