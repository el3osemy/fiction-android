package at.yawk.fiction.android.provider.local;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.ui.QueryEditorFragment;

/**
 * @author yawkat
 */
public class LocalQueryEditorFragment extends QueryEditorFragment<LocalSearchQuery> {
    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        View root = inflater.inflate(R.layout.query_editor_local, container, false);

        bindChoice((Spinner) root.findViewById(R.id.order),
                   LocalSearchQuery::getOrder,
                   LocalSearchQuery::setOrder,
                   StoryOrder::getName,
                   StoryOrder.values());
        bindBoolean((CheckBox) root.findViewById(R.id.readNone),
                    LocalSearchQuery::isReadNone,
                    LocalSearchQuery::setReadNone);
        bindBoolean((CheckBox) root.findViewById(R.id.readSome),
                    LocalSearchQuery::isReadSome,
                    LocalSearchQuery::setReadSome);
        bindBoolean((CheckBox) root.findViewById(R.id.readAll),
                    LocalSearchQuery::isReadAll,
                    LocalSearchQuery::setReadAll);
        bindBoolean((CheckBox) root.findViewById(R.id.downloadedNone),
                    LocalSearchQuery::isDownloadedNone,
                    LocalSearchQuery::setDownloadedNone);
        bindBoolean((CheckBox) root.findViewById(R.id.downloadedSome),
                    LocalSearchQuery::isDownloadedSome,
                    LocalSearchQuery::setDownloadedSome);
        bindBoolean((CheckBox) root.findViewById(R.id.downloadedAll),
                    LocalSearchQuery::isDownloadedAll,
                    LocalSearchQuery::setDownloadedAll);

        return root;
    }
}
