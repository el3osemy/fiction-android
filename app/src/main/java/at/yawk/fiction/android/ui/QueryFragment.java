package at.yawk.fiction.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import at.yawk.fiction.Chapter;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.*;
import at.yawk.fiction.android.download.DownloadManager;
import at.yawk.fiction.android.download.task.StoryListUpdateTask;
import at.yawk.fiction.android.event.StoryUpdateEvent;
import at.yawk.fiction.android.event.Subscribe;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.android.storage.StoryWrapper;
import butterknife.Bind;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ContentView(R.layout.query_story_list)
public class QueryFragment extends ContentViewFragment implements AdapterView.OnItemClickListener {
    @Inject ProviderManager providerManager;
    @Inject Toasts toasts;
    @Inject TaskManager taskManager;
    @Inject FragmentUiRunner uiRunner;
    @Inject DownloadManager downloadManager;

    @Bind(R.id.storyList) ListView storyList;
    @Bind(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;

    private final TaskContext taskContext = new TaskContext();

    private View footerView;

    private final WeakBiMap<StoryWrapper, View> storyViewMap = new WeakBiMap<>();

    private QueryWrapper query;

    Worker currentWorker;

    public void setQuery(QueryWrapper query) {
        Bundle args = new Bundle();
        args.putParcelable("query", WrapperParcelable.objectToParcelable(query));
        setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        query = WrapperParcelable.parcelableToObject(getArguments().getParcelable("query"));
    }

    private void initWorker() {
        currentWorker = new Worker();
        storyList.setAdapter(currentWorker.adapter);
        currentWorker.checkFetchMore();
    }

    @Subscribe(Subscribe.EventQueue.UI)
    public void onStoryUpdate(StoryUpdateEvent event) {
        if (getActivity() == null) { return; }
        uiRunner.runOnUiThread(() -> {
            View view = storyViewMap.getByKey(event.getStory());
            if (view != null) {
                decorateEntry(event.getStory(), view);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && currentWorker != null) {
            currentWorker.checkFetchMore();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        refreshLayout.setOnRefreshListener(() -> {
            refreshLayout.setRefreshing(false);
            initWorker();
        });

        storyList.setOnItemClickListener(this);

        footerView = getActivity().getLayoutInflater().inflate(R.layout.query_overscroll, storyList, false);
        storyList.addFooterView(footerView, null, false);

        initWorker();

        storyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                currentWorker.checkFetchMore();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StoryWrapper wrapper = (StoryWrapper) parent.getItemAtPosition(position);
        if (wrapper == null) { return; }
        Intent intent = new Intent(getActivity(), StoryActivity.class);
        intent.putExtra("story", WrapperParcelable.objectToParcelable(wrapper.getStory()));
        startActivity(intent);
    }

    private void decorateEntry(StoryWrapper wrapper, View view) {
        ((TextView) view.findViewById(R.id.storyTitle)).setText(wrapper.getStory().getTitle());

        TextView readChapterDisplay = (TextView) view.findViewById(R.id.readChapterDisplay);
        List<? extends Chapter> chapters = wrapper.getStory().getChapters();
        readChapterDisplay.setText(wrapper.getReadChapterCount() + "/" +
                                   (chapters == null ? "?" : Integer.toString(chapters.size())));
        //noinspection deprecation
        readChapterDisplay.setTextColor(getResources().getColor(wrapper.getReadProgressType().getColorResource()));

        TextView downloadedChapterDisplay = (TextView) view.findViewById(R.id.downloadedChapterDisplay);
        downloadedChapterDisplay.setText(wrapper.getDownloadedChapterCount() + "/");

        storyViewMap.put(wrapper, view);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.query_story_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.updateAll:
            downloadManager.enqueue(new StoryListUpdateTask(new ArrayList<>(currentWorker.stories)));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    ///////////////////////////////

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    private class Worker {
        private final List<StoryWrapper> stories = new CopyOnWriteArrayList<>();
        private final Pageable<StoryWrapper> pageable;
        private final SimpleArrayAdapter<StoryWrapper> adapter;

        private int page = 0;
        private int pageCount = -1;
        private int failedAttemptCount = 0;
        private boolean hasMore = true;
        private boolean fetching = false;

        Worker() {
            pageable = providerManager.getProvider(query.getQuery()).searchWrappers(query.getQuery());
            adapter = new SimpleArrayAdapter<StoryWrapper>(getActivity(), R.layout.query_entry, stories) {
                @Override
                protected void decorateView(View view, int position) {
                    decorateEntry(getItem(position), view);
                }
            };
        }

        synchronized void checkFetchMore() {
            if (fetching || !isVisible() || !isValid()) { return; }
            if (hasMore) {
                if (storyList.getLastVisiblePosition() >= stories.size() - 1) {
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

        /**
         * @return <code>true</code> if the page was fetched successfully, <code>false</code> otherwise.
         */
        private boolean fetchOne() {
            boolean ok = false;
            try {
                log.trace("Fetching page {}", this.page);
                Pageable.Page<StoryWrapper> page = pageable.getPage(this.page);
                log.trace("Fetched, merging with database");
                pageCount = page.getPageCount();

                List<StoryWrapper> additions = new ArrayList<>(page.getEntries()); // eager copy

                log.trace("Done, passing on to UI");
                stories.addAll(additions);
                hasMore = !page.isLast();
                runOnUiThread(adapter::notifyDataSetChanged);
                ok = true;
            } catch (Throwable e) {
                log.error("Failed to fetch page {}", page, e);
                if (isValid()) {
                    toasts.toast("Failed to fetch page {}", page, e);
                }
            }
            if (ok) {
                page++;
                failedAttemptCount = 0;
            } else {
                failedAttemptCount++;
            }
            return ok;
        }

        private void updateLoading() {
            runOnUiThread(() -> {
                footerView.findViewById(R.id.loading_progress).setVisibility(fetching ? View.VISIBLE : View.GONE);

                String pageString;
                if (hasMore) {
                    pageString = (page + 1) + " / " + (pageCount == -1 ? "?" : pageCount);
                } else {
                    pageString = Integer.toString(page);
                }
                ((TextView) footerView.findViewById(R.id.loading_page))
                        .setText(pageString);

                ((TextView) footerView.findViewById(R.id.loading_failure_count))
                        .setText(failedAttemptCount == 0 ? "" : "[" + failedAttemptCount + "]");
            });
        }

        private void runOnUiThread(Runnable task) {
            if (isValid()) { uiRunner.runOnUiThread(task); }
        }

        boolean isValid() {
            return QueryFragment.this.currentWorker == this;
        }
    }
}
