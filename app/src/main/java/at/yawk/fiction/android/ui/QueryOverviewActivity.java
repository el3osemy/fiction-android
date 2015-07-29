package at.yawk.fiction.android.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.impl.fanfiction.FfnCategory;
import at.yawk.fiction.impl.fanfiction.FfnSearchQuery;
import at.yawk.fiction.impl.fanfiction.FfnSubCategory;
import java.util.Collections;
import java.util.List;
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

        queryArrayAdapter = new ArrayAdapter<QueryWrapper>(this, android.R.layout.simple_list_item_1, queries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ((TextView) convertView).setText(getItem(position).getName());
                return convertView;
            }
        };

        DrawerLayout drawerParent = (DrawerLayout) findViewById(R.id.drawer_layout);

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
}
