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
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.QueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import java.util.*;
import javax.annotation.Nullable;
import javax.inject.Inject;
import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

/**
 * @author yawkat
 */
@ContentView(R.layout.query_wrapper_editor)
public class QueryWrapperActivity extends RoboFragmentActivity {
    @Inject ProviderManager providerManager;
    @Inject QueryManager queryManager;

    private QueryWrapper query;

    private Map<AndroidFictionProvider, SearchQuery> queriesByProvider = new HashMap<>();
    private QueryEditorFragment queryEditorFragment;

    @InjectView(R.id.accept) Button acceptButton;
    @InjectView(R.id.remove) Button removeButton;
    @InjectView(R.id.cancel) Button cancelButton;
    @InjectView(R.id.queryName) EditText queryName;
    @InjectView(R.id.provider) Spinner providerSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parcelable queryParcel = getIntent().getParcelableExtra("query");
        if (queryParcel != null) {
            query = WrapperParcelable.parcelableToObject(queryParcel);
            AndroidFictionProvider provider = providerManager.getProvider(query.getQuery());
            queriesByProvider.put(provider, query.getQuery());
            selectProvider(provider);
            acceptButton.setText(R.string.update_query);
            removeButton.setVisibility(View.VISIBLE);
        } else {
            query = new QueryWrapper();
            query.setId(UUID.randomUUID());
            acceptButton.setText(R.string.create_query);
            removeButton.setVisibility(View.GONE);
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

        updateSavable();

        acceptButton.setOnClickListener(v -> {
            updateSavable();
            if (isSavable()) {
                save();
                finish();
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
