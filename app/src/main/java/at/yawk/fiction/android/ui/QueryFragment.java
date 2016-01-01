package at.yawk.fiction.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
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
import at.yawk.fiction.android.download.task.QueryDownloadTask;
import at.yawk.fiction.android.download.task.StoryListUpdateTask;
import at.yawk.fiction.android.event.StoryUpdateEvent;
import at.yawk.fiction.android.event.Subscribe;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.OfflineQueryManager;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import butterknife.Bind;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Uninterruptibles;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ContentView(R.layout.query_story_list)
public class QueryFragment extends ContentViewFragment implements AdapterView.OnItemClickListener {
    @Inject ProviderManager providerManager;
    @Inject Toasts toasts;
    @Inject TaskManager taskManager;
    @Inject FragmentUiRunner uiRunner;
    @Inject DownloadManager downloadManager;
    @Inject OfflineQueryManager offlineQueryManager;
    @Inject SharedPreferences sharedPreferences;
    @Inject StoryManager storyManager;

    @Bind(R.id.storyList) ListView storyList;
    @Bind(R.id.refreshLayout) SwipeRefreshLayout refreshLayout;

    private final TaskContext taskContext = new TaskContext();

    private View footerView;

    private final WeakBiMap<StoryWrapper, View> storyViewMap = new WeakBiMap<>();

    @Nullable Worker currentWorker;

    @Nullable Pattern searchQuery = null;

    public void setQuery(QueryWrapper query) {
        Bundle args = new Bundle();
        args.putParcelable("query", WrapperParcelable.objectToParcelable(query));
        setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    private void initWorker(QueryWrapper query) {
        AndroidFictionProvider provider = providerManager.getProvider(query.getQuery());
        boolean offlineCacheable = provider.isQueryOfflineCacheable(query.getQuery());
        boolean offline = offlineCacheable &&
                          sharedPreferences.getBoolean("offline_mode", false);

        try {
            currentWorker = new Worker(provider, query, offline, !offline && offlineCacheable);
            storyList.setAdapter(currentWorker.adapter);
            currentWorker.checkFetchMore();
        } catch (Throwable t) {
            // in case the provider throws something somewhere
            log.error("Failed to initialize download worker", t);
            toasts.toast("Failed to initialize download worker", t);
        }
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

        QueryWrapper query = WrapperParcelable.parcelableToObject(getArguments().getParcelable("query"));

        refreshLayout.setOnRefreshListener(() -> {
            refreshLayout.setRefreshing(false);
            initWorker(query);
        });

        storyList.setOnItemClickListener(this);

        footerView = getActivity().getLayoutInflater().inflate(R.layout.query_overscroll, storyList, false);
        storyList.addFooterView(footerView, null, false);

        initWorker(query);

        storyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (currentWorker != null) {
                    currentWorker.checkFetchMore();
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StoryWrapper wrapper = (StoryWrapper) parent.getItemAtPosition(position);
        if (wrapper == null) { return; }
        startActivity(StoryActivity.createLaunchIntent(getActivity(), wrapper));
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

    private boolean canFetchPages() {
        return currentWorker != null &&
               currentWorker.provider.isQueryOfflineCacheable(currentWorker.query.getQuery());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.query_story_list, menu);
        menu.findItem(R.id.fetchPages).setEnabled(canFetchPages());
        ((SearchView) menu.findItem(R.id.filter).getActionView())
                .setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        setSearchQuery(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        setSearchQuery(newText);
                        return true;
                    }

                    private void setSearchQuery(String queryPattern) {
                        searchQuery = queryPattern.isEmpty() ? null :
                                Pattern.compile(queryPattern, Pattern.CASE_INSENSITIVE);
                        if (currentWorker != null) {
                            currentWorker.stories.notifyFilterChanged();
                            currentWorker.adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.updateAll:
            if (currentWorker != null) {
                downloadManager.enqueue(new StoryListUpdateTask(new ArrayList<>(currentWorker.stories)));
            }
            return true;
        case R.id.fetchPages:
            if (currentWorker == null || !canFetchPages()) {
                toasts.toast("Cannot fetch query of this provider");
            } else {
                int pageCount = currentWorker.pageCount;
                if (pageCount < 0) {
                    toasts.toast("Cannot fetch pages because we don't know how many there are!");
                } else {
                    downloadManager.enqueue(new QueryDownloadTask(currentWorker.query, pageCount));
                }
            }
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
        private final FilteredStoryList stories = new FilteredStoryList();
        private final Pageable<StoryWrapper> pageable;
        private final SimpleArrayAdapter<StoryWrapper> adapter;

        private final AndroidFictionProvider provider;
        private final QueryWrapper query;
        private final boolean savePagesToOfflineCache;

        private int page = 0;
        private int pageCount = -1;
        private int failedAttemptCount = 0;
        private boolean hasMore = true;
        private boolean fetching = false;

        Worker(AndroidFictionProvider provider, QueryWrapper query, boolean offline, boolean savePagesToOfflineCache) {
            this.provider = provider;
            this.query = query;
            this.savePagesToOfflineCache = savePagesToOfflineCache;
            if (offline) {
                pageable = offlineQueryManager.load(query);
            } else {
                pageable = provider.searchWrappers(query.getQuery());
            }
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
                            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                        }
                        synchronized (QueryFragment.this) {
                            fetching = false;
                            checkFetchMore();
                        }
                    });
                } else {
                    log.trace("Last item not visible, not fetching more");
                }
                updateLoading();
            }
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
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    updateLoading();
                });
                if (savePagesToOfflineCache) {
                    offlineQueryManager.save(query, this.page, page);
                }
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

    @ThreadSafe
    private class FilteredStoryList extends AbstractList<StoryWrapper> {
        private final List<LazyStory> allStories = new CopyOnWriteArrayList<>();
        private List<LazyStory> filteredStories = new CopyOnWriteArrayList<>();

        private boolean accept(LazyStory story) {
            if (searchQuery != null && !searchQuery.matcher(story.getTitle()).find()) {
                return false;
            }
            return true;
        }

        public synchronized void notifyFilterChanged() {
            List<LazyStory> filteredLocal = new ArrayList<>();
            for (LazyStory story : allStories) {
                if (accept(story)) {
                    filteredLocal.add(story);
                }
            }
            filteredStories = new CopyOnWriteArrayList<>(filteredLocal);
        }

        @Override
        public synchronized boolean add(StoryWrapper object) {
            LazyStory lazy = new LazyStory(object);
            allStories.add(lazy);
            if (accept(lazy)) { filteredStories.add(lazy); }
            return true;
        }

        @Override
        public StoryWrapper get(int location) {
            return filteredStories.get(location).getItem();
        }

        @Override
        public int size() {
            return filteredStories.size();
        }

        @Override
        public Iterator<StoryWrapper> iterator() {
            return Iterators.transform(filteredStories.iterator(), LazyStory::getItem);
        }
    }

    private class LazyStory {
        private final String id;
        @Getter private final String title;
        private Reference<StoryWrapper> item;

        LazyStory(StoryWrapper wrapper) {
            id = wrapper.getId();
            title = wrapper.getStory().getTitle();
            item = makeReference(wrapper);
        }

        public StoryWrapper getItem() {
            StoryWrapper cached = item.get();
            if (cached == null) {
                cached = storyManager.getStory(id);
                item = makeReference(cached);
            }
            return cached;
        }

        private Reference<StoryWrapper> makeReference(StoryWrapper wrapper) {
            return new SoftReference<>(wrapper);
        }
    }
}
