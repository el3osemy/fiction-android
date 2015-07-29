package at.yawk.fiction.android.provider.ffn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fanfiction.FfnCategory;
import at.yawk.fiction.impl.fanfiction.FfnSearchQuery;
import at.yawk.fiction.impl.fanfiction.FfnSubCategory;

/**
 * @author yawkat
 */
public class FfnQueryEditorFragment extends QueryEditorFragment<FfnSearchQuery> {
    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        View editor = inflater.inflate(R.layout.query_editor_ffn, container, false);

        bindChoice((Spinner) editor.findViewById(R.id.category),
                   query -> getSubCategory(query).getCategory(),
                   (query, choice) -> getSubCategory(query).setCategory(choice),
                   FfnCategory::getName,
                   FfnCategory.values());

        return editor;
    }

    private FfnSubCategory getSubCategory(FfnSearchQuery q) {
        if (q.getCategory() == null) {
            FfnSubCategory category = new FfnSubCategory();
            category.setCategory(FfnCategory.ANIME);
            q.setCategory(category);
        }
        return q.getCategory();
    }
}
