package at.yawk.fiction.android.provider.fim;

import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.FragmentUiRunner;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.inject.SupportFragmentModule;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fimfiction.*;
import butterknife.Bind;
import dagger.Module;
import java.util.Collections;
import java.util.List;
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

    @Bind(R.id.order) Spinner order;
    @Bind(R.id.publishTime) Spinner publishTime;
    @Bind(R.id.status) Spinner status;
    @Bind(R.id.minWords) EditText minWords;
    @Bind(R.id.maxWords) EditText maxWords;
    @Bind(R.id.shelf) Spinner shelf;

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
    }

    private void bindShelves(List<FimShelf> shelves) {
        bindChoice(shelf,
                   FimSearchQuery::getShelf,
                   FimSearchQuery::setShelf,
                   s -> s == null ? "Any Shelf" : s.getName(),
                   andNull(shelves));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = SupportFragmentModule.class, injects = FimQueryEditorFragment.class)
    static class M {}
}
