package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.storage.QueryWrapper;
import java.util.*;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
public class QueryWrapperActivity extends FragmentActivity implements ContextProvider {
    private QueryWrapper query;

    private Map<AndroidFictionProvider, SearchQuery> queriesByProvider = new HashMap<>();
    private QueryEditorFragment queryEditorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.query_wrapper_editor);

        Parcelable queryParcel = getIntent().getParcelableExtra("query");
        if (queryParcel != null) {
            query = getContext().parcelableToObject(queryParcel);
            AndroidFictionProvider provider = getContext().getProviderManager().getProvider(query.getQuery());
            queriesByProvider.put(provider, query.getQuery());
            selectProvider(provider);
            ((Button) findViewById(R.id.accept)).setText(R.string.update_query);
        } else {
            query = new QueryWrapper();
            query.setId(UUID.randomUUID());
            ((Button) findViewById(R.id.accept)).setText(R.string.create_query);
        }

        ((EditText) findViewById(R.id.queryName)).setText(query.getName());

        List<AndroidFictionProvider> providers = new ArrayList<>(getContext().getProviderManager().getProviders());
        Spinner providerSpinner = (Spinner) findViewById(R.id.provider);
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

        findViewById(R.id.accept).setOnClickListener(v -> {
            updateSavable();
            if (isSavable()) {
                save();
                finish();
            }
        });
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
    }

    private void save() {
        assert isSavable();

        query.setName(((EditText) findViewById(R.id.queryName)).getText().toString());
        if (queryEditorFragment != null) {
            queryEditorFragment.save();
            query.setQuery(queryEditorFragment.getQuery());
        }

        getContext().getStorageManager().getQueryManager().saveQuery(query);
    }

    private void selectProvider(@Nullable AndroidFictionProvider provider) {
        if (provider != null) {
            SearchQuery query = queriesByProvider.get(provider);
            if (query == null) {
                queriesByProvider.put(provider, query = provider.createQuery());
            }

            queryEditorFragment = provider.createQueryEditorFragment();
            //noinspection unchecked
            queryEditorFragment.setQuery(getContext(), query);

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
        findViewById(R.id.accept).setEnabled(isSavable());
    }

    private boolean isSavable() {
        return queryEditorFragment != null && queryEditorFragment.isSavable();
    }

    @Override
    public FictionContext getContext() {
        return FictionContext.get(this);
    }
}
