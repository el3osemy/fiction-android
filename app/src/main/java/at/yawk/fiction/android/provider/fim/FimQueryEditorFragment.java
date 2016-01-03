package at.yawk.fiction.android.provider.fim;

import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.FragmentUiRunner;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fimfiction.*;
import butterknife.Bind;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_editor_fim)
public class FimQueryEditorFragment extends QueryEditorFragment<FimSearchQuery> {
    private TaskContext taskContext = new TaskContext();

    @Inject TaskManager taskManager;
    @Inject FimAndroidFictionProvider fimAndroidFictionProvider;
    @Inject Toasts toasts;
    @Inject FragmentUiRunner uiRunner;

    @Nullable
    private List<FimTag> tags = null;

    @Bind(R.id.order) Spinner order;
    @Bind(R.id.publishTime) Spinner publishTime;
    @Bind(R.id.status) Spinner status;
    @Bind(R.id.minWords) EditText minWords;
    @Bind(R.id.maxWords) EditText maxWords;
    @Bind(R.id.shelf) Spinner shelf;
    @Bind(R.id.unread) CheckBox unread;
    @Bind(R.id.tagList) ViewGroup tagList;
    @Bind(R.id.addTag) View addTag;

    private Set<FimTag> includedTags;
    private Set<FimTag> excludedTags;

    @Override
    protected void bind() {
        bindChoice(order,
                   FimSearchQuery::getOrder,
                   FimSearchQuery::setOrder,
                   order -> order == null ? "Default Order" : order.name(),
                   andNull(FimOrder.values()));
        bindChoice(publishTime,
                   FimSearchQuery::getPublishTime,
                   FimSearchQuery::setPublishTime,
                   timeRange -> timeRange == null ? "Any Publish Time" : timeRange.name(),
                   andNull(FimTimeRange.values()));
        bindChoice(status,
                   FimSearchQuery::getStatus,
                   FimSearchQuery::setStatus,
                   status -> status == null ? "Any Status" : status.name(),
                   andNull(FimStatus.values()));
        bindInteger(minWords,
                    FimSearchQuery::getMinWords,
                    FimSearchQuery::setMinWords);
        bindInteger(maxWords,
                    FimSearchQuery::getMaxWords,
                    FimSearchQuery::setMaxWords);

        bindBoolean(unread,
                    FimSearchQuery::getUnread,
                    // not checked = we don't care
                    (q, unread) -> q.setUnread(unread ? true : null));

        bindShelves(Collections.emptyList());

        taskManager.execute(taskContext, () -> {
            List<FimShelf> shelves;
            try {
                shelves = fimAndroidFictionProvider.getFictionProvider().fetchShelves();
            } catch (Exception e) {
                log.warn("Could not fetch shelves", e);
                toasts.toast("Could not fetch shelves", e);
                return;
            }

            uiRunner.runOnUiThread(() -> bindShelves(shelves));
        });

        includedTags = new HashSet<>();
        if (getQuery().getIncludedTags() != null) { includedTags.addAll(getQuery().getIncludedTags()); }
        excludedTags = new HashSet<>();
        if (getQuery().getExcludedTags() != null) { excludedTags.addAll(getQuery().getExcludedTags()); }
        addTag.setOnClickListener(v -> openTagAdder());
        updateTags();
    }

    private void openTagAdder() {
        if (tags == null) {
            taskManager.execute(taskContext, () -> {
                try {
                    tags = fimAndroidFictionProvider.getFictionProvider().fetchTags();
                } catch (Exception e) {
                    log.error("Failed to fetch tags", e);
                    toasts.toast("Failed to fetch tags", e);
                }
                uiRunner.runOnUiThread(this::openTagAdder);
            });
            return;
        }
        if (tags.isEmpty()) {
            toasts.toast("No tags found");
            return;
        }

        Set<FimTag> selected = new HashSet<>();
        String[] tagNames = Lists.transform(tags, FimTag::getName).toArray(new String[tags.size()]);

        new AlertDialog.Builder(getActivity())
                .setMultiChoiceItems(tagNames, new boolean[tags.size()], (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selected.add(tags.get(which));
                    } else {
                        selected.remove(tags.get(which));
                    }
                })
                .setTitle(R.string.filter_tag)
                .setPositiveButton(R.string.include, (dg, which) -> {
                    excludedTags.removeAll(selected);
                    includedTags.addAll(selected);
                    updateTags();
                })
                .setNegativeButton(R.string.exclude, (dg, which) -> {
                    includedTags.removeAll(selected);
                    excludedTags.addAll(selected);
                    updateTags();
                })
                .setNeutralButton(R.string.cancel, (dg, which) -> {
                    dg.dismiss();
                })
                .show();
    }

    private void updateTags() {
        // remove all children except the addTag button
        tagList.removeViews(0, tagList.getChildCount() - 1);

        for (FimTag included : includedTags) {
            TextView view = (TextView) getActivity().getLayoutInflater()
                    .inflate(R.layout.query_editor_fim_tag, tagList, false);
            view.setBackgroundColor(0xff1b5e20);
            view.setText(included.getName());
            view.setOnClickListener(v -> {
                includedTags.remove(included);
                updateTags();
            });
            tagList.addView(view, tagList.getChildCount() - 1);
        }

        for (FimTag excluded : excludedTags) {
            TextView view = (TextView) getActivity().getLayoutInflater()
                    .inflate(R.layout.query_editor_fim_tag, tagList, false);
            view.setBackgroundColor(0xffb71c1c);
            view.setText(excluded.getName());
            view.setOnClickListener(v -> {
                excludedTags.remove(excluded);
                updateTags();
            });
            tagList.addView(view, tagList.getChildCount() - 1);
        }
    }

    private void bindShelves(List<FimShelf> shelves) {
        bindChoice(shelf,
                   FimSearchQuery::getShelf,
                   FimSearchQuery::setShelf,
                   s -> s == null ? "Any Shelf" : s.getName(),
                   andNull(shelves));
    }

    @Override
    public void save() {
        super.save();
        getQuery().setExcludedTags(excludedTags);
        getQuery().setIncludedTags(includedTags);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }
}
