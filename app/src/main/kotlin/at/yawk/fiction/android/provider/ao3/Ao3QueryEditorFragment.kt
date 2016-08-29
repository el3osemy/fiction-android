package at.yawk.fiction.android.provider.ao3

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import at.yawk.fiction.android.R
import at.yawk.fiction.android.context.FragmentUiRunner
import at.yawk.fiction.android.context.TaskContext
import at.yawk.fiction.android.context.TaskManager
import at.yawk.fiction.android.inject.ContentView
import at.yawk.fiction.android.ui.QueryEditorFragment
import at.yawk.fiction.android.ui.StringArrayAdapter
import at.yawk.fiction.impl.ao3.*
import butterknife.Bind
import org.apmem.tools.layouts.FlowLayout
import java.util.*
import javax.inject.Inject

/**
 * @author yawkat
 */
private fun String.orNull() = if (this.isBlank()) null else this

private fun Ao3SearchQuery.Range?.withMin(nMin: Int?) = if (nMin == null) {
    if (this == null || this.max == null) null
    else Ao3SearchQuery.Range(null, max)
} else Ao3SearchQuery.Range(nMin, this?.max)

private fun Ao3SearchQuery.Range?.withMax(nMax: Int?) = if (nMax == null) {
    if (this == null || this.min == null) null
    else Ao3SearchQuery.Range(this.min, null)
} else Ao3SearchQuery.Range(this?.min, nMax)

@ContentView(R.layout.query_editor_ao3)
class Ao3QueryEditorFragment : QueryEditorFragment<Ao3SearchQuery>() {
    private val taskContext = TaskContext()

    @Inject lateinit var ao3Provider: Ao3AndroidFictionProvider
    @Inject lateinit var fragmentUiRunner: FragmentUiRunner
    @Inject lateinit var taskManager: TaskManager

    lateinit var currentView: View

    @Suppress("UNCHECKED_CAST")
    private fun <V: View> view(@IdRes id: Int): Lazy<V> = lazy {
        currentView.findViewById(id) as V
    }

    val order by view<Spinner>(R.id.order)
    val orderDirection by view<Spinner>(R.id.orderDirection)
    val title by view<EditText>(R.id.title)
    val freeText by view<EditText>(R.id.freeText)
    val author by view<EditText>(R.id.author)
    val period by view<EditText>(R.id.period)
    val complete by view<CheckBox>(R.id.complete)
    val singleChapter by view<CheckBox>(R.id.singleChapter)
    val rating by view<Spinner>(R.id.rating)

    val fandoms by view<FlowLayout>(R.id.fandoms)
    val addFandom by view<Button>(R.id.addFandom)
    val tags by view<FlowLayout>(R.id.tags)
    val addTag by view<Button>(R.id.addTag)

    val minWords by view<EditText>(R.id.minWords)
    val maxWords by view<EditText>(R.id.maxWords)
    val minHits by view<EditText>(R.id.minHits)
    val maxHits by view<EditText>(R.id.maxHits)
    val minKudos by view<EditText>(R.id.minKudos)
    val maxKudos by view<EditText>(R.id.maxKudos)
    val minComments by view<EditText>(R.id.minComments)
    val maxComments by view<EditText>(R.id.maxComments)
    val minBookmarks by view<EditText>(R.id.minBookmarks)
    val maxBookmarks by view<EditText>(R.id.maxBookmarks)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentView = view
        super.onViewCreated(view, savedInstanceState)
    }

    override fun bind() {
        bindChoice(order,
                { it.order }, { q, v -> q.order = v },
                { it?.name ?: "Default Order" },
                andNull(Ao3SearchOrder.values()))
        bindChoice(orderDirection,
                { it.orderDirection }, { q, v -> q.orderDirection = v },
                { it?.name ?: "Default Order Direction" },
                andNull(OrderDirection.values()))
        bindString(title, { it.title }, { q, v -> q.title = v.orNull() })
        bindString(freeText, { it.freeText }, { q, v -> q.freeText = v.orNull() })
        bindString(author, { it.author }, { q, v -> q.author = v.orNull() })
        bindString(period, { it.period }, { q, v -> q.period = v.orNull() })
        bindBoolean(complete, { it.isComplete }, { q, v -> q.isComplete = v })
        bindBoolean(singleChapter, { it.isSingleChapter }, { q, v -> q.isSingleChapter = v })
        bindChoice(rating,
                { it.rating }, { q, v -> q.rating = v },
                { it?.name ?: "Default Rating" },
                andNull(Ao3Rating.values()))
        bindInteger(minWords, { it.wordCount?.min }, { q, v -> q.wordCount = q.wordCount.withMin(v) })
        bindInteger(maxWords, { it.wordCount?.max }, { q, v -> q.wordCount = q.wordCount.withMax(v) })
        bindInteger(minHits, { it.hits?.min }, { q, v -> q.hits = q.hits.withMin(v) })
        bindInteger(maxHits, { it.hits?.max }, { q, v -> q.hits = q.hits.withMax(v) })
        bindInteger(minKudos, { it.kudos?.min }, { q, v -> q.kudos = q.kudos.withMin(v) })
        bindInteger(maxKudos, { it.kudos?.max }, { q, v -> q.kudos = q.kudos.withMax(v) })
        bindInteger(minComments, { it.comments?.min }, { q, v -> q.comments = q.comments.withMin(v) })
        bindInteger(maxComments, { it.comments?.max }, { q, v -> q.comments = q.comments.withMax(v) })
        bindInteger(minBookmarks, { it.bookmarks?.min }, { q, v -> q.bookmarks = q.bookmarks.withMin(v) })
        bindInteger(maxBookmarks, { it.bookmarks?.max }, { q, v -> q.bookmarks = q.bookmarks.withMax(v) })

        bindTags(fandoms, addFandom, "Add Fandom",
                { ao3Provider.fictionProvider.suggestFandoms(it) },
                { it.fandoms }, { q, v -> q.fandoms = v })
        bindTags(tags, addTag, "Add Tag",
                { ao3Provider.fictionProvider.suggestTags(it) },
                { it.tags }, { q, v -> q.tags = v })
    }

    private fun bindTags(
            pane: FlowLayout,
            add: Button,
            addTitle: String,
            suggest: (String) -> List<Ao3Tag>,
            get: (Ao3SearchQuery) -> Set<Ao3Tag>?,
            set: (Ao3SearchQuery, Set<Ao3Tag>?) -> Unit
    ) {
        fun update() {
            pane.removeViews(0, pane.childCount - 1)

            val tags = get(query) ?: emptySet()
            for (tag in tags) {
                val view = activity.layoutInflater.inflate(R.layout.query_editor_fim_tag, pane, false) as TextView
                view.text = tag.name
                view.setOnClickListener { v ->
                    val newTags = tags - tag
                    set(query, if (newTags.isEmpty()) null else newTags)
                    update()
                }
                pane.addView(view, pane.childCount - 1)
            }
        }

        update()

        add.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(addTitle)
            val selector = activity.layoutInflater.inflate(R.layout.query_editor_ao3_tag, null)
            builder.setView(selector)

            var dialog: AlertDialog? = null
            val suggestionAdapter = object : StringArrayAdapter<Ao3Tag>(activity, ArrayList(), { it!!.name }) {
                override fun decorateView(view: View, position: Int) {
                    super.decorateView(view, position)
                    view.setOnClickListener {
                        set(query, (get(query) ?: emptySet()) + getItem(position))
                        dialog?.dismiss()
                        update()
                    }
                }
            }
            (selector.findViewById(R.id.suggestions) as ListView).adapter = suggestionAdapter

            (selector.findViewById(R.id.query) as EditText).addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                }

                var version = 0

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val commitVersion = ++version
                    taskManager.execute(taskContext) {
                        val suggestions = suggest(s.toString())
                        fragmentUiRunner.runOnUiThread {
                            if (commitVersion == version) {
                                suggestionAdapter.clear()
                                suggestionAdapter.addAll(suggestions)
                            }
                        }
                    }
                }
            })

            dialog = builder.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        taskContext.destroy()
    }
}