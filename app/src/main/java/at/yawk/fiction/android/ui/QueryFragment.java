package at.yawk.fiction.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.yawk.fiction.Pageable;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.storage.QueryWrapper;
import at.yawk.fiction.android.storage.StoryWrapper;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryFragment extends ListFragment implements ContextProvider {
    private final TaskContext taskContext = new TaskContext();
    private final List<StoryWrapper> stories = new CopyOnWriteArrayList<>();

    private View footerView;
    private QueryWrapper query;
    private FictionContext context;

    public static QueryFragment create(FictionContext context, QueryWrapper query) {
        QueryFragment fragment = new QueryFragment();
        Bundle args = new Bundle();
        args.putParcelable("query", context.objectToParcelable(query));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        query = getContext().parcelableToObject(getArguments().getParcelable("query"));

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

        pageable = getContext().getProviderManager().getProvider(query.getQuery()).search(query.getQuery());

        footerView = getActivity().getLayoutInflater().inflate(R.layout.query_overscroll, null);
        updateLoading();
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

        checkFetchMore();
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                checkFetchMore();
            }
        });
        getListView().addFooterView(footerView);
    }

    private void updateEntries() {
        getActivity().runOnUiThread(((ArrayAdapter<?>) getListAdapter())::notifyDataSetChanged);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        StoryWrapper wrapper = (StoryWrapper) l.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), StoryActivity.class);
        intent.putExtra("story", getContext().objectToParcelable(wrapper.getStory()));
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

    @Override
    public FictionContext getContext() {
        if (context == null) { context = FictionContext.get(this); }
        return context;
    }

    ///////////////////////////////

    private Pageable<? extends Story> pageable;
    private int page;
    private int pageCount = -1;
    private int failedAttemptCount = 0;
    private boolean hasMore = true;
    private boolean fetching = false;

    private synchronized void checkFetchMore() {
        updateLoading();
        if (fetching || !isVisible()) { return; }
        if (hasMore) {
            ListView listView = getListView();
            if (listView.getLastVisiblePosition() >= stories.size() - 1) {
                fetching = true;
                getContext().getTaskManager().execute(taskContext, () -> {
                    if (!fetchOne()) {
                        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                    }
                    synchronized (QueryFragment.this) {
                        fetching = false;
                        updateLoading();
                        checkFetchMore();
                    }
                });
            } else {
                log.trace("Last item not visible, not fetching more");
            }
        }
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
            Pageable.Page<? extends Story> page = pageable.getPage(this.page);
            pageCount = page.getPageCount();

            log.trace("Entries {}", page.getEntries());

            List<StoryWrapper> additions = new ArrayList<>(page.getEntries().size());
            for (Story story : page.getEntries()) {
                StoryWrapper wrapper = getContext().getStorageManager().mergeStory(story);
                additions.add(wrapper);
            }
            stories.addAll(additions);
            hasMore = !page.isLast();
            updateEntries();
            ok = true;
        } catch (Exception e) {
            log.error("Failed to fetch page {}", page, e);
            getContext().toast(getActivity(), "Failed to fetch page {}", page, e);
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
