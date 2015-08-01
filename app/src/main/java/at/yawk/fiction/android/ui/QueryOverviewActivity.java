package at.yawk.fiction.android.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import at.yawk.fiction.android.Importer;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.storage.QueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_overview)
public class QueryOverviewActivity extends RoboFragmentActivity {
    @Inject QueryManager queryManager;
    @Inject Importer importer;

    private ArrayAdapter<QueryWrapper> queryArrayAdapter;

    @InjectView(R.id.left_drawer) ListView drawer;
    @InjectView(R.id.drawer_layout) DrawerLayout drawerParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log.info("Creating query overview");

        queryArrayAdapter = new StringArrayAdapter<>(this, new ArrayList<>(),
                                                     QueryOverviewActivity::getName);

        updateQueries(true);

        drawer.setAdapter(queryArrayAdapter);

        drawer.setOnItemClickListener((parent, view, position, id) -> {
            showQuery(queryArrayAdapter.getItem(position), true);
            drawerParent.closeDrawers();
        });
        drawer.setOnItemLongClickListener((parent, view, position, id) -> {
            editQuery(queryArrayAdapter.getItem(position));
            return true;
        });
    }

    private void updateQueries(boolean first) {
        List<QueryWrapper> queries = queryManager.getQueries();
        queryArrayAdapter.clear();
        queryArrayAdapter.addAll(queries);

        for (QueryWrapper query : queries) {
            if (query.getId().equals(queryManager.getSelectedQueryId())) {
                showQuery(query, first);
                break;
            }
        }
    }

    private void showQuery(QueryWrapper query, boolean forceRedraw) {
        if (!forceRedraw && query.getId().equals(queryManager.getSelectedQueryId())) {
            return;
        }

        log.info("ShowQuery {}", query);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        QueryFragment fragment = new QueryFragment();
        fragment.setQuery(query);
        ft.replace(R.id.content_frame, fragment);
        ft.commit();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getName(query));
        }

        queryManager.setSelectedQueryId(query.getId());
    }

    private static String getName(QueryWrapper query) {
        String name = query.getName();
        return name == null || name.isEmpty() ? "Unnamed Query" : name;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            updateQueries(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.query_overview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.create_query:
            editQuery(null);
            return true;
        case R.id.import_ffn:
            new Thread(importer).start();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editQuery(@Nullable QueryWrapper query) {
        Intent intent = new Intent(this, QueryWrapperActivity.class);
        if (query != null) {
            intent.putExtra("query", WrapperParcelable.objectToParcelable(query));
        }
        startActivity(intent);
    }
}
