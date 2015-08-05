package at.yawk.fiction.android.provider.local;

import android.widget.CheckBox;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.inject.FragmentModule;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import butterknife.Bind;
import dagger.Module;

/**
 * @author yawkat
 */
@ContentView(R.layout.query_editor_local)
public class LocalQueryEditorFragment extends QueryEditorFragment<LocalSearchQuery> {
    @Bind(R.id.order) Spinner orderView;
    @Bind(R.id.readNone) CheckBox readNoneView;
    @Bind(R.id.readSome) CheckBox readSomeView;
    @Bind(R.id.readAll) CheckBox readAllView;
    @Bind(R.id.downloadedNone) CheckBox downloadedNoneView;
    @Bind(R.id.downloadedSome) CheckBox downloadedSomeView;
    @Bind(R.id.downloadedAll) CheckBox downloadedAllView;

    @Override
    protected void bind() {
        bindChoice(orderView,
                   LocalSearchQuery::getOrder,
                   LocalSearchQuery::setOrder,
                   StoryOrder::getName,
                   StoryOrder.values());
        bindBoolean(readNoneView,
                    LocalSearchQuery::isReadNone,
                    LocalSearchQuery::setReadNone);
        bindBoolean(readSomeView,
                    LocalSearchQuery::isReadSome,
                    LocalSearchQuery::setReadSome);
        bindBoolean(readAllView,
                    LocalSearchQuery::isReadAll,
                    LocalSearchQuery::setReadAll);
        bindBoolean(downloadedNoneView,
                    LocalSearchQuery::isDownloadedNone,
                    LocalSearchQuery::setDownloadedNone);
        bindBoolean(downloadedSomeView,
                    LocalSearchQuery::isDownloadedSome,
                    LocalSearchQuery::setDownloadedSome);
        bindBoolean(downloadedAllView,
                    LocalSearchQuery::isDownloadedAll,
                    LocalSearchQuery::setDownloadedAll);
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = FragmentModule.class, injects = LocalQueryEditorFragment.class)
    static class M {}
}
