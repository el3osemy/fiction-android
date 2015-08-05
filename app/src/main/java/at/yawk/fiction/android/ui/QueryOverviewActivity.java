package at.yawk.fiction.android.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import at.yawk.fiction.android.Cleanup;
import at.yawk.fiction.android.Importer;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.storage.QueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import butterknife.Bind;
import com.mobeta.android.dslv.DragSortListView;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_overview)
public class QueryOverviewActivity extends FragmentActivity {
    @Inject QueryManager queryManager;
    @Inject Importer importer;
    @Inject Cleanup cleanup;

    private ArrayAdapter<QueryWrapper> queryArrayAdapter;

    @Bind(R.id.left_drawer) DragSortListView drawer;
    @Bind(R.id.drawer_layout) DrawerLayout drawerParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.initActivity(this);

        log.info("Creating query overview");

        queryArrayAdapter = new SimpleArrayAdapter<QueryWrapper>(
                this, R.layout.query_overview_query_item, new ArrayList<>()) {
            @Override
            protected void decorateView(View view, int position) {
                ((TextView) view.findViewById(R.id.queryName)).setText(getItem(position).getName());
            }
        };

        updateQueries(true);

        drawer.setAdapter(queryArrayAdapter);

        drawer.setOnItemClickListener((parent, view, position, id) -> {
            showQuery(queryArrayAdapter.getItem(position), true);
            drawerParent.closeDrawers();
        });
        drawer.setOnItemLongClickListener((parent, view, position, id) -> {
            longClickQuery(position);
            return true;
        });
        drawer.setDropListener((from, to) -> {
            queryManager.moveQuery(from, to);
            updateQueries(false);
        });
        drawer.setDragEnabled(false);
    }

    private void longClickQuery(int position) {
        QueryWrapper contextQuery = queryArrayAdapter.getItem(position);
        startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(contextQuery.getName());
                mode.getMenuInflater().inflate(R.menu.query_context, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                case R.id.editQuery:
                    editQuery(contextQuery);
                    return true;
                case R.id.reorder:
                    drawer.setDragEnabled(true);
                    item.setVisible(false);
                    return true;
                default:
                    return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        });
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        drawer.setDragEnabled(false);
    }

    private void updateQueries(boolean forceQueryReload) {
        List<QueryWrapper> queries = queryManager.getQueries();
        queryArrayAdapter.clear();
        queryArrayAdapter.addAll(queries);

        for (QueryWrapper query : queries) {
            if (query.getId().equals(queryManager.getSelectedQueryId())) {
                showQuery(query, forceQueryReload);
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
        case R.id.cleanup_text:
            new Thread(cleanup).start();
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
