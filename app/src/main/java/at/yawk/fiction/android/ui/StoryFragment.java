package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import at.yawk.fiction.*;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yawkat
 */
@Slf4j
public class StoryFragment extends Fragment implements ContextProvider {
    private TaskContext taskContext = new TaskContext();

    private StoryWrapper wrapper;
    private FictionContext fictionContext;
    private View root;
    private ViewGroup chapterGroup;

    @Override
    public FictionContext getContext() {
        if (fictionContext == null) { fictionContext = FictionContext.get(getActivity()); }
        return fictionContext;
    }

    public static StoryFragment create(FictionContext context, StoryWrapper story) {
        StoryFragment fragment = new StoryFragment();
        Bundle args = new Bundle();
        args.putParcelable("story", context.objectToParcelable(story.getStory()));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wrapper = getContext().getStorageManager()
                .getStory(getContext().<Story>parcelableToObject(getArguments().getParcelable("story")));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.story, container, false);
        chapterGroup = (ViewGroup) root.findViewById(R.id.chapters);
        root.findViewById(R.id.title).setOnClickListener(v -> getContext().getTaskManager().execute(taskContext, () -> {
            try {
                getContext().getStorageManager().getEpubBuilder().openEpub(getActivity(), wrapper.getStory());
            } catch (Exception e) {
                log.error("Failed to open epub", e);
                getContext().toast(getActivity(), "Failed to open epub", e);
            }
        }));
        refresh();
        return root;
    }

    private List<ChapterHolder> chapterHolders = new ArrayList<>();

    private void refresh() {
        ((TextView) root.findViewById(R.id.title)).setText(wrapper.getStory().getTitle());
        ((TextView) root.findViewById(R.id.author)).setText(wrapper.getStory().getAuthor().getName());
        AndroidFictionProvider provider = getContext().getProviderManager().getProvider(wrapper.getStory());
        ((TextView) root.findViewById(R.id.tags)).setText(
                StringUtils.join(provider.getTags(wrapper.getStory()), " • "));

        TextView descriptionView = (TextView) root.findViewById(R.id.description);
        FormattedText description = wrapper.getStory().getDescription();
        if (description instanceof HtmlText) {
            descriptionView.setText(Html.fromHtml(((HtmlText) description).getHtml()));
        } else if (description instanceof RawText) {
            descriptionView.setText(((RawText) description).getText());
        } else {
            descriptionView.setText("");
        }


        List<? extends Chapter> chapters = wrapper.getStory().getChapters();
        for (int i = 0; i < chapters.size(); i++) {
            ChapterHolder holder;
            if (i >= chapterHolders.size()) {
                View view = getActivity().getLayoutInflater().inflate(R.layout.chapter, chapterGroup, false);
                chapterGroup.addView(view);
                chapterHolders.add(holder = new ChapterHolder(view, i));
            } else {
                holder = chapterHolders.get(i);
            }

            holder.setChapter(chapters.get(i));
        }

        if (chapters.size() < chapterHolders.size()) {
            List<ChapterHolder> toRemove = chapterHolders.subList(chapters.size(), chapterHolders.size());
            for (int i = 0; i < toRemove.size(); i++) {
                chapterGroup.removeViewAt(chapters.size() + i);
            }
            toRemove.clear();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    private class ChapterHolder {
        private final View view;
        private final int index;
        private Chapter chapter;

        private final CheckBox readBox;

        ChapterHolder(View view, int index) {
            this.view = view;
            this.index = index;

            readBox = (CheckBox) view.findViewById(R.id.chapterRead);
            readBox.setOnCheckedChangeListener((buttonView, isChecked) -> wrapper.setChapterRead(chapter, isChecked));

            View.OnClickListener refreshListener = v -> fetchChapter();
            view.findViewById(R.id.chapterDownload).setOnClickListener(refreshListener);
            view.findViewById(R.id.chapterRefresh).setOnClickListener(refreshListener);
        }

        void setChapter(Chapter chapter) {
            this.chapter = chapter;

            String name = chapter.getName();
            if (name == null) {
                name = "Chapter " + (index + 1);
            }
            ((TextView) view.findViewById(R.id.chapterName)).setText(name);

            boolean hasText = chapter.getText() != null;

            setChapterViewStatus(hasText ? R.id.chapterRefresh : R.id.chapterDownload);

            readBox.setVisibility(hasText ? View.VISIBLE : View.INVISIBLE);
            readBox.setChecked(wrapper.isChapterRead(chapter));
        }

        void fetchChapter() {
            setChapterViewStatus(R.id.chapterDownloading);

            Story storyClone = getContext().getStorageManager().getPojoMerger().clone(wrapper.getStory());
            Chapter chapter = storyClone.getChapters().get(index);
            AndroidFictionProvider provider = getContext().getProviderManager().getProvider(storyClone);
            getContext().getTaskManager().execute(taskContext, () -> {
                try {
                    provider.fetchChapter(storyClone, chapter);
                    getContext().getStorageManager().getTextStorage().externalize(chapter);

                    getContext().getStorageManager().mergeStory(storyClone);
                    getActivity().runOnUiThread(StoryFragment.this::refresh);
                } catch (Exception e) {
                    log.error("Failed to fetch chapter", e);
                    getContext().toast(getActivity(), "Failed to fetch chapter", e);
                }
            });
        }

        private void setChapterViewStatus(int statusId) {
            ViewGroup group = (ViewGroup) view.findViewById(R.id.downloadWrapper);
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                child.setVisibility(child.getId() == statusId ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }
}
