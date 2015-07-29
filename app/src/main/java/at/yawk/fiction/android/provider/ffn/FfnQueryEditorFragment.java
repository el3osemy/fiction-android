package at.yawk.fiction.android.provider.ffn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.android.ui.StringArrayAdapter;
import at.yawk.fiction.impl.fanfiction.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class FfnQueryEditorFragment extends QueryEditorFragment<FfnSearchQuery> {
    private TaskContext taskContext = new TaskContext();

    private SubCategoryOrder subCategoryOrder = SubCategoryOrder.SIZE;
    private ArrayAdapter<FfnSubCategory> subCategoryArrayAdapter;
    private View editor;

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup container) {
        editor = inflater.inflate(R.layout.query_editor_ffn, container, false);

        subCategoryArrayAdapter = new StringArrayAdapter<>(
                getActivity(), new ArrayList<>(), FfnSubCategory::getName);

        ((Spinner) editor.findViewById(R.id.subCategory)).setAdapter(subCategoryArrayAdapter);
        ((Spinner) editor.findViewById(R.id.category)).setAdapter(
                new StringArrayAdapter<>(getActivity(), FfnCategory.values(), FfnCategory::getName));
        ((Spinner) editor.findViewById(R.id.category)).setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        subCategoryArrayAdapter.clear();
                        getContext().getTaskManager().execute(taskContext, () -> {
                            FfnCategory category = (FfnCategory) parent.getSelectedItem();
                            try {
                                List<FfnSubCategory> subCategories =
                                        getContext().getProviderManager().findProvider(FfnAndroidFictionProvider.class)
                                                .getFictionProvider().fetchSubCategories(category);
                                Collections.sort(subCategories, subCategoryOrder);
                                getActivity().runOnUiThread(() -> subCategoryArrayAdapter.addAll(subCategories));
                            } catch (Exception e) {
                                log.error("Failed to fetch subcategories for {}", category, e);
                                getContext().toast(getActivity(), "Failed to fetch subcategories", e);
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        subCategoryArrayAdapter.clear();
                    }
                });

        ((Spinner) editor.findViewById(R.id.subCategoryOrder)).setAdapter(
                new StringArrayAdapter<>(getActivity(), SubCategoryOrder.values(), SubCategoryOrder::getName));
        ((Spinner) editor.findViewById(R.id.subCategoryOrder)).setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        subCategoryOrder = (SubCategoryOrder) parent.getSelectedItem();
                        subCategoryArrayAdapter.sort(subCategoryOrder);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        bindChoice((Spinner) editor.findViewById(R.id.searchOrder),
                   FfnSearchQuery::getOrder,
                   FfnSearchQuery::setOrder,
                   FfnSearchOrder::getName,
                   FfnSearchOrder.values());
        this.<FfnRating>bindChoice((Spinner) editor.findViewById(R.id.ratingMin),
                                   FfnSearchQuery::getMinRating,
                                   FfnSearchQuery::setMinRating,
                                   FfnRating::getName,
                                   FfnRating.values());
        this.<FfnRating>bindChoice((Spinner) editor.findViewById(R.id.ratingMax),
                                   FfnSearchQuery::getMaxRating,
                                   FfnSearchQuery::setMaxRating,
                                   FfnRating::getName,
                                   FfnRating.values());

        ArrayList<TimeRange> ranges = new ArrayList<>();
        ranges.add(null);
        ranges.addAll(Arrays.asList(TimeRange.values()));
        bindChoice((Spinner) editor.findViewById(R.id.timeRange),
                   FfnSearchQuery::getTimeRange,
                   FfnSearchQuery::setTimeRange,
                   timeRange -> timeRange == null ? "All" : timeRange.getLabel(),
                   ranges);

        return editor;
    }

    @Override
    public void save() {
        super.save();
        getQuery().setCategory((FfnSubCategory) ((Spinner) editor.findViewById(R.id.subCategory)).getSelectedItem());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    @RequiredArgsConstructor
    private enum SubCategoryOrder implements Comparator<FfnSubCategory> {
        SIZE("Size") {
            @Override
            public int compare(FfnSubCategory lhs, FfnSubCategory rhs) {
                return rhs.getEstimatedStoryCount() - lhs.getEstimatedStoryCount();
            }
        },
        ALPHABETICAL("Alph") {
            @Override
            public int compare(FfnSubCategory lhs, FfnSubCategory rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        };

        @lombok.Getter private final String name;
    }
}
