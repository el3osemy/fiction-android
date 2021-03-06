package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.QueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import butterknife.Bind;
import java.util.*;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_wrapper_editor)
@ContentViewActivity.Dialog
public class QueryWrapperActivity extends ContentViewActivity {
    @Inject ProviderManager providerManager;
    @Inject QueryManager queryManager;
    @Inject Toasts toasts;

    private final QueryWrapper query = new QueryWrapper();

    private Map<AndroidFictionProvider, SearchQuery> queriesByProvider = new HashMap<>();
    private QueryEditorFragment queryEditorFragment;

    @Bind(R.id.accept) Button acceptButton;
    @Bind(R.id.remove) Button removeButton;
    @Bind(R.id.cancel) Button cancelButton;
    @Bind(R.id.queryName) EditText queryName;
    @Bind(R.id.provider) Spinner providerSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parcelable queryParcel = getIntent().getParcelableExtra("query");
        AndroidFictionProvider firstProvider = null;
        if (queryParcel != null) {
            UUID id = WrapperParcelable.parcelableToObject(queryParcel);
            for (QueryWrapper query : queryManager.getQueries()) {
                if (query.getId().equals(id)) {
                    this.query.setId(query.getId());
                    this.query.setName(query.getName());
                    this.query.setQuery(query.getQuery());
                    break;
                }
            }
        }

        if (query.getId() == null) {
            query.setId(UUID.randomUUID());
            acceptButton.setText(R.string.create_query);
            removeButton.setVisibility(View.GONE);
        } else {
            firstProvider = providerManager.getProvider(query.getQuery());
            queriesByProvider.put(firstProvider, query.getQuery());
            acceptButton.setText(R.string.update_query);
            removeButton.setVisibility(View.VISIBLE);
        }

        queryName.setText(query.getName());

        List<AndroidFictionProvider> providers = new ArrayList<>(providerManager.getProviders());
        providerSpinner.setAdapter(new StringArrayAdapter<>(this, providers, AndroidFictionProvider::getName));
        providerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectProvider((AndroidFictionProvider) providerSpinner.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectProvider(null);
            }
        });
        providerSpinner.setSelection(providers.indexOf(firstProvider));

        updateSavable();

        acceptButton.setOnClickListener(v -> {
            updateSavable();
            if (isSavable()) {
                try {
                    save();
                    finish();
                } catch (Throwable t) {
                    log.error("Failed to save query", t);
                    toasts.toast("Failed to save query", t);
                }
            }
        });
        cancelButton.setOnClickListener(v -> finish());
        removeButton.setOnClickListener(v -> {
            queryManager.removeQuery(query);
            finish();
        });
    }

    private void save() {
        assert isSavable();

        query.setName(queryName.getText().toString());
        if (queryEditorFragment != null) {
            queryEditorFragment.save();
            query.setQuery(queryEditorFragment.getQuery());
        }

        queryManager.saveQuery(query);
    }

    private void selectProvider(@Nullable AndroidFictionProvider provider) {
        if (provider != null) {
            SearchQuery query = queriesByProvider.get(provider);
            if (query == null) {
                queriesByProvider.put(provider, query = provider.createQuery());
            }

            queryEditorFragment = provider.createQueryEditorFragment();
            //noinspection unchecked
            queryEditorFragment.setQuery(query);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.providerQueryEditor, queryEditorFragment)
                    .commit();
        } else {
            if (queryEditorFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(queryEditorFragment)
                        .commit();
            }

            queryEditorFragment = null;
        }

        updateSavable();
    }

    void updateSavable() {
        acceptButton.setEnabled(isSavable());
    }

    private boolean isSavable() {
        return queryEditorFragment != null && queryEditorFragment.isSavable();
    }
}
