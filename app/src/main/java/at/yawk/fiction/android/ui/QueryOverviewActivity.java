package at.yawk.fiction.android.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.impl.fanfiction.FfnCategory;
import at.yawk.fiction.impl.fanfiction.FfnSearchQuery;
import at.yawk.fiction.impl.fanfiction.FfnSubCategory;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class QueryOverviewActivity extends FragmentActivity implements ContextProvider {
    private List<QueryWrapper> queries;
    private ArrayAdapter<?> queryArrayAdapter;

    @Override
    public FictionContext getContext() {
        return FictionContext.get(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.query_overview);

        log.info("Creating query overview");

        {
            // todo editable query list
            FfnSearchQuery query = new FfnSearchQuery();
            FfnSubCategory category = new FfnSubCategory();
            category.setCategory(FfnCategory.GAMES);
            category.setName("Elder Scroll series");
            query.setCategory(category);

            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.setName("Test");
            queryWrapper.setQuery(query);
            queries = Collections.singletonList(queryWrapper);
        }

        queryArrayAdapter = new StringArrayAdapter<>(this, queries, QueryWrapper::getName);

        DrawerLayout drawerParent = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawerParent.setDrawerListener(this);

        ListView drawer = (ListView) findViewById(R.id.left_drawer);
        drawer.setAdapter(queryArrayAdapter);
        drawer.setOnItemClickListener((parent, view, position, id) -> {
            showQuery(queries.get(position));
            drawerParent.closeDrawers();
        });

        showQuery(queries.get(0)); // todo save position
    }

    private void showQuery(QueryWrapper query) {
        log.info("ShowQuery {}", query);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, QueryFragment.create(getContext(), query));
        ft.commit();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(query.getName());
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void editQuery(@Nullable SearchQuery query) {
        Intent intent = new Intent(this, QueryWrapperActivity.class);
        if (query != null) {
            intent.putExtra("query", getContext().objectToParcelable(query));
        }
        startActivity(intent);
    }
}
