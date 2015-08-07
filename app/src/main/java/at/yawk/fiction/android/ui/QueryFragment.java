package at.yawk.fiction.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.*;
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

    @Bind(R.id.storyList) ListView storyList;

    private final TaskContext taskContext = new TaskContext();
    private final List<StoryWrapper> stories = new CopyOnWriteArrayList<>();

    private View footerView;

    private final WeakBiMap<StoryWrapper, View> storyViewMap = new WeakBiMap<>();

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

        pageable = providerManager.getProvider(query.getQuery()).searchWrappers(query.getQuery());
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
        if (!hidden) {
            checkFetchMore();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        storyList.setAdapter(new SimpleArrayAdapter<StoryWrapper>(getActivity(), R.layout.query_entry, stories) {
            @Override
            protected void decorateView(View view, int position) {
                decorateEntry(getItem(position), view);
            }
        });
        storyList.setOnItemClickListener(this);

        footerView = getActivity().getLayoutInflater().inflate(R.layout.query_overscroll, storyList, false);
        storyList.addFooterView(footerView, null, false);

        checkFetchMore();

        storyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                checkFetchMore();
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

        TextView downloadedChapterDisplay = (TextView) view.findViewById(R.id.downloadedChapterDisplay);
        downloadedChapterDisplay.setText(wrapper.getDownloadedChapterCount() + "/");

        storyViewMap.put(wrapper, view);
    }

    ///////////////////////////////

    private Pageable<StoryWrapper> pageable;
    private int page;
    private int pageCount = -1;
    private int failedAttemptCount = 0;
    private boolean hasMore = true;
    private boolean fetching = false;

    private synchronized void checkFetchMore() {
        if (fetching || !isVisible()) { return; }
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

    private void updateLoading() {
        uiRunner.runOnUiThread(() -> {
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
            uiRunner.runOnUiThread(((ArrayAdapter<?>) storyList.getAdapter())::notifyDataSetChanged);
            ok = true;
        } catch (Throwable e) {
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
