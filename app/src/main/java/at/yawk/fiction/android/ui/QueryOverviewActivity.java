package at.yawk.fiction.android.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import at.yawk.fiction.android.Cleanup;
import at.yawk.fiction.android.Importer;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.event.QueryListUpdateEvent;
import at.yawk.fiction.android.event.Subscribe;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.storage.QueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.android.storage.RootFile;
import butterknife.Bind;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.query_overview)
@ContentViewActivity.NoActionBar
public class QueryOverviewActivity extends ContentViewActivity {
    private static final int PERM_REQUEST = 1;

    @Inject QueryManager queryManager;
    @Inject Importer importer;
    @Inject Cleanup cleanup;

    @Bind(R.id.queryList) ViewGroup queryList;
    @Bind(R.id.drawer_layout) DrawerLayout drawerParent;
    @Bind(R.id.createQuery) View createQuery;
    @Bind(R.id.settings) View settings;
    @Bind(R.id.downloads) View downloads;
    @Bind(R.id.toolbar) Toolbar toolbar;

    private ActionMode actionMode;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void inject() {
        if (RootFile.hasPermission(this)) {
            super.inject();
            onCreate0();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
        case PERM_REQUEST:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                RootFile.hasPermission(this); // will run callbacks
                startActivity(Intent.makeRestartActivityTask(getComponentName()));
                finish();
            }
        }
    }

    private void onCreate0() {
        setSupportActionBar(toolbar);

        updateQueries(true);

        createQuery.setOnClickListener(v -> editQuery(null));
        settings.setOnClickListener(v -> startActivity(new Intent(this, MainPreferenceActivity.class)));
        downloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadManagerActivity.class)));

        drawerToggle = new ActionBarDrawerToggle(
                this, drawerParent, toolbar, R.string.open_queries, R.string.close_queries) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        };
        drawerParent.setDrawerListener(drawerToggle);
        drawerParent.setStatusBarBackgroundColor(((ColorDrawable) toolbar.getBackground()).getColor());
    }

    private void longClickQuery(QueryWrapper query) {
        actionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle(getName(query));
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
                    editQuery(query);
                    return true;
                case R.id.up:
                    List<QueryWrapper> queries = queryManager.getQueries();
                    int oldIndex = queries.indexOf(query);
                    if (oldIndex > 0) {
                        queryManager.moveQuery(oldIndex, oldIndex - 1);
                    }
                    return true;
                case R.id.down:
                    queries = queryManager.getQueries();
                    oldIndex = queries.indexOf(query);
                    if (oldIndex < queries.size() - 1) {
                        queryManager.moveQuery(oldIndex, oldIndex + 1);
                    }
                    return true;
                default:
                    return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
            }
        });
    }

    private void updateQueries(boolean forceQueryReload) {
        List<QueryWrapper> queries = queryManager.getQueries();

        for (int i = 0; i < queries.size(); i++) {
            View view;
            if (queryList.getChildCount() <= i) {
                view = getLayoutInflater().inflate(R.layout.query_overview_query_item, queryList);
            } else {
                view = queryList.getChildAt(i);
            }
            QueryWrapper query = queries.get(i);
            ((TextView) view.findViewById(R.id.queryName)).setText(getName(query));
            view.setSelected(query.getId().equals(queryManager.getSelectedQueryId()));

            view.setOnLongClickListener(v -> {
                longClickQuery(query);
                return true;
            });
            view.setOnClickListener(v -> {
                showQuery(query, true);
                drawerParent.closeDrawers();
            });
        }

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

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        QueryFragment fragment = new QueryFragment();
        fragment.setQuery(query);
        ft.replace(R.id.content_frame, fragment);
        ft.commit();

        setTitle(getName(query));

        queryManager.setSelectedQueryId(query.getId());
    }

    private static String getName(QueryWrapper query) {
        String name = query.getName();
        return name == null || name.isEmpty() ? "Unnamed Query" : name;
    }

    @Subscribe
    public void onQueryListUpdated(QueryListUpdateEvent event) {
        updateQueries(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.query_overview, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.import_ffn:
            new Thread(importer).start();
            return true;
        case R.id.cleanup_text:
            new Thread(cleanup).start();
            return true;
        }
        return drawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (RootFile.hasPermission(this)) {
            drawerToggle.syncState();
        }
    }

    private void editQuery(@Nullable QueryWrapper query) {
        Intent intent = new Intent(this, QueryWrapperActivity.class);
        if (query != null) {
            intent.putExtra("query", WrapperParcelable.objectToParcelable(query.getId()));
        }
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
}
