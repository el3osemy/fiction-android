package at.yawk.fiction.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import roboguice.fragment.RoboListFragment;

@Slf4j
public class QueryFragment extends RoboListFragment {
    @Inject ProviderManager providerManager;
    @Inject Toasts toasts;
    @Inject TaskManager taskManager;
    @Inject StorageManager storageManager;

    private final TaskContext taskContext = new TaskContext();
    private final List<StoryWrapper> stories = new CopyOnWriteArrayList<>();

    private View footerView;
    private QueryWrapper query;

    public void setQuery(QueryWrapper query) {
        Bundle args = new Bundle();
        args.putParcelable("query", WrapperParcelable.objectToParcelable(query));
        setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        query = WrapperParcelable.parcelableToObject(getArguments().getParcelable("query"));

        setListAdapter(new ArrayAdapter<StoryWrapper>(getActivity(), R.layout.query_entry, stories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(R.layout.query_entry, parent, false);
                }
                decorateEntry(getItem(position), convertView);
                return convertView;
            }
        });

        pageable = providerManager.getProvider(query.getQuery()).search(query.getQuery());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            checkFetchMore();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        footerView = getActivity().getLayoutInflater().inflate(R.layout.query_overscroll, getListView(), false);
        getListView().addFooterView(footerView);

        checkFetchMore();

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                checkFetchMore();
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        StoryWrapper wrapper = (StoryWrapper) l.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), StoryActivity.class);
        intent.putExtra("story", WrapperParcelable.objectToParcelable(wrapper.getStory()));
        startActivity(intent);
    }

    private void decorateEntry(StoryWrapper wrapper, View view) {
        ((TextView) view.findViewById(R.id.storyTitle)).setText(wrapper.getStory().getTitle());

        TextView readChapterDisplay = (TextView) view.findViewById(R.id.readChapterDisplay);
        int chapterCount = wrapper.getStory().getChapters().size();
        int readChapterCount = wrapper.getReadChapterCount();
        readChapterDisplay.setText(readChapterCount + "/" + chapterCount);
        if (readChapterCount >= chapterCount) {
            readChapterDisplay.setTextColor(getResources().getColor(R.color.chaptersReadAll));
        } else if (readChapterCount > 0) {
            readChapterDisplay.setTextColor(getResources().getColor(R.color.chaptersReadSome));
        } else {
            readChapterDisplay.setTextColor(getResources().getColor(R.color.chaptersReadNone));
        }
    }

    ///////////////////////////////

    private Pageable<? extends Story> pageable;
    private int page;
    private int pageCount = -1;
    private int failedAttemptCount = 0;
    private boolean hasMore = true;
    private boolean fetching = false;

    private synchronized void checkFetchMore() {
        if (fetching || !isVisible()) { return; }
        if (hasMore) {
            ListView listView = getListView();
            if (listView.getLastVisiblePosition() >= stories.size() - 1) {
                fetching = true;
                taskManager.execute(taskContext, () -> {
                    if (!fetchOne()) {
                        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                    }
                    synchronized (QueryFragment.this) {
                        fetching = false;
                        checkFetchMore();
                    }
                });
            } else {
                log.trace("Last item not visible, not fetching more");
            }
        }
        updateLoading();
    }

    private void updateLoading() {
        getActivity().runOnUiThread(() -> {
            footerView.findViewById(R.id.loading_progress).setVisibility(fetching ? View.VISIBLE : View.GONE);
            ((TextView) footerView.findViewById(R.id.loading_page))
                    .setText((page + 1) + " / " + (pageCount == -1 ? "?" : pageCount));
            ((TextView) footerView.findViewById(R.id.loading_failure_count))
                    .setText(failedAttemptCount == 0 ? "" : "[" + failedAttemptCount + "]");
        });
    }

    /**
     * @return <code>true</code> if the page was fetched successfully, <code>false</code> otherwise.
     */
    private boolean fetchOne() {
        boolean ok = false;
        try {
            log.trace("Fetching page {}", this.page);
            Pageable.Page<? extends Story> page = pageable.getPage(this.page);
            log.trace("Fetched, merging with database");
            //Debug.startMethodTracingSampling("fetch", 8 * 1024 * 1024, 1000);
            pageCount = page.getPageCount();

            List<StoryWrapper> additions = new ArrayList<>(page.getEntries().size());
            for (Story story : page.getEntries()) {
                StoryWrapper wrapper = storageManager.mergeStory(story);
                additions.add(wrapper);
            }
            //Debug.stopMethodTracing();
            log.trace("Done, passing on to UI");
            stories.addAll(additions);
            hasMore = !page.isLast();
            getActivity().runOnUiThread(((ArrayAdapter<?>) getListAdapter())::notifyDataSetChanged);
            ok = true;
        } catch (Exception e) {
            log.error("Failed to fetch page {}", page, e);
            toasts.toast("Failed to fetch page {}", page, e);
        }
        if (ok) {
            page++;
            failedAttemptCount = 0;
        } else {
            failedAttemptCount++;
        }
        return ok;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }
}
