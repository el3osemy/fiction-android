package at.yawk.fiction.android.provider.local;

import android.widget.CheckBox;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

/**
 * @author yawkat
 */
@ContentView(R.layout.query_editor_local)
public class LocalQueryEditorFragment extends QueryEditorFragment<LocalSearchQuery> {
    @InjectView(R.id.order) Spinner orderView;
    @InjectView(R.id.readNone) CheckBox readNoneView;
    @InjectView(R.id.readSome) CheckBox readSomeView;
    @InjectView(R.id.readAll) CheckBox readAllView;
    @InjectView(R.id.downloadedNone) CheckBox downloadedNoneView;
    @InjectView(R.id.downloadedSome) CheckBox downloadedSomeView;
    @InjectView(R.id.downloadedAll) CheckBox downloadedAllView;

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
}
