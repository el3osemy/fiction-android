package at.yawk.fiction.android.provider.ffn;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.inject.FragmentModule;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.android.ui.StringArrayAdapter;
import at.yawk.fiction.impl.fanfiction.*;
import butterknife.Bind;
import dagger.Module;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_editor_ffn)
public class FfnQueryEditorFragment extends QueryEditorFragment<FfnSearchQuery> {
    @Inject TaskManager taskManager;
    //@Inject Toasts toasts;
    @Inject FfnAndroidFictionProvider provider;

    private TaskContext taskContext = new TaskContext();

    private SubCategoryOrder subCategoryOrder = SubCategoryOrder.SIZE;
    @Nullable private ArrayAdapter<FfnSubCategory> subCategoryArrayAdapter;

    @Bind(R.id.subCategory) Spinner subCategoryView;
    @Bind(R.id.category) Spinner categoryView;
    @Bind(R.id.subCategoryOrder) Spinner subCategoryOrderView;
    @Bind(R.id.searchOrder) Spinner searchOrderView;
    @Bind(R.id.ratingMin) Spinner ratingMinView;
    @Bind(R.id.ratingMax) Spinner ratingMaxView;
    @Bind(R.id.timeRange) Spinner timeRangeView;
    @Bind(R.id.status) Spinner statusView;

    @Override
    protected void bind() {
        subCategoryView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSavable();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateSavable();
            }
        });

        categoryView.setAdapter(new StringArrayAdapter<>(getActivity(), FfnCategory.values(), FfnCategory::getName));
        categoryView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                taskManager.execute(taskContext, () -> {
                    FfnCategory category = (FfnCategory) parent.getSelectedItem();
                    try {
                        List<FfnSubCategory> subCategories =
                                provider.getFictionProvider().fetchSubCategories(category);
                        Collections.sort(subCategories, subCategoryOrder);
                        getActivity().runOnUiThread(() -> {
                            subCategoryArrayAdapter = new StringArrayAdapter<>(
                                    getActivity(), subCategories, FfnSubCategory::getName);
                            subCategoryView.setAdapter(subCategoryArrayAdapter);
                            FfnSubCategory subCategory = getQuery().getCategory();
                            if (subCategory != null) {
                                for (int i = 0; i < subCategories.size(); i++) {
                                    if (subCategory.getName().equals(subCategories.get(i).getName())) {
                                        subCategoryView.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.error("Failed to fetch subcategories for {}", category, e);
                        //toasts.toast("Failed to fetch subcategories", e);
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                subCategoryView.setAdapter(null);
            }
        });
        if (getQuery().getCategory() != null) {
            categoryView.setSelection(Arrays.asList(FfnCategory.values())
                                              .indexOf(getQuery().getCategory().getCategory()));
        }

        subCategoryOrderView.setAdapter(
                new StringArrayAdapter<>(getActivity(), SubCategoryOrder.values(), SubCategoryOrder::getName));
        subCategoryOrderView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                subCategoryOrder = (SubCategoryOrder) parent.getSelectedItem();
                if (subCategoryArrayAdapter != null) {
                    subCategoryArrayAdapter.sort(subCategoryOrder);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        bindChoice(searchOrderView,
                   FfnSearchQuery::getOrder,
                   FfnSearchQuery::setOrder,
                   FfnSearchOrder::getName,
                   FfnSearchOrder.values());
        bindChoice(ratingMinView,
                   FfnSearchQuery::getMinRating,
                   FfnSearchQuery::setMinRating,
                   FfnRating::getName,
                   FfnRating.values());
        bindChoice(ratingMaxView,
                   FfnSearchQuery::getMaxRating,
                   FfnSearchQuery::setMaxRating,
                   FfnRating::getName,
                   FfnRating.values());

        bindChoice(timeRangeView,
                   FfnSearchQuery::getTimeRange,
                   FfnSearchQuery::setTimeRange,
                   timeRange -> timeRange == null ? "All" : timeRange.getLabel(),
                   andNull(TimeRange.values()));

        bindChoice(statusView,
                   FfnSearchQuery::getStatus,
                   FfnSearchQuery::setStatus,
                   status -> status == null ? "All" : status.getName(),
                   andNull(FfnStatus.values()));
    }

    @Override
    public void save() {
        super.save();
        getQuery().setCategory(getSelectedSubCategory());
    }

    @Nullable
    private FfnSubCategory getSelectedSubCategory() {
        return subCategoryView == null ? null : (FfnSubCategory) subCategoryView.getSelectedItem();
    }

    @Override
    public boolean isSavable() {
        return super.isSavable() && getSelectedSubCategory() != null;
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

    @Module(addsTo = FragmentModule.class, injects = FfnQueryEditorFragment.class)
    static class M {}

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
