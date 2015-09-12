package at.yawk.fiction.android.provider.fim;

import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.inject.FragmentModule;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fimfiction.FimOrder;
import at.yawk.fiction.impl.fimfiction.FimSearchQuery;
import at.yawk.fiction.impl.fimfiction.FimStatus;
import at.yawk.fiction.impl.fimfiction.FimTimeRange;
import butterknife.Bind;
import dagger.Module;

/**
 * @author yawkat
 */
@ContentView(R.layout.query_editor_fim)
public class FimQueryEditorFragment extends QueryEditorFragment<FimSearchQuery> {
    @Bind(R.id.order) Spinner order;
    @Bind(R.id.publishTime) Spinner publishTime;
    @Bind(R.id.status) Spinner status;
    @Bind(R.id.minWords) EditText minWords;
    @Bind(R.id.maxWords) EditText maxWords;

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
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = FragmentModule.class, injects = FimQueryEditorFragment.class)
    static class M {}
}
