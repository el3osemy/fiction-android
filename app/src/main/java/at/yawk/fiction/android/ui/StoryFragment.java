package at.yawk.fiction.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;
import at.yawk.fiction.*;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.EpubBuilder;
import at.yawk.fiction.android.storage.PojoMerger;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.story)
public class StoryFragment extends ContentViewFragment {
    @Inject Toasts toasts;
    @Inject StorageManager storageManager;
    @Inject TaskManager taskManager;
    @Inject EpubBuilder epubBuilder;
    @Inject ProviderManager providerManager;
    @Inject PojoMerger pojoMerger;

    private TaskContext taskContext = new TaskContext();

    private StoryWrapper wrapper;

    @InjectView(R.id.chapters) ViewGroup chapterGroup;
    @InjectView(R.id.title) TextView titleView;
    @InjectView(R.id.author) TextView authorView;
    @InjectView(R.id.tags) TextView tagsView;
    @InjectView(R.id.description) TextView descriptionView;

    public void setStory(StoryWrapper wrapper) {
        Bundle args = new Bundle();
        args.putParcelable("story", WrapperParcelable.objectToParcelable(wrapper.getStory()));
        setArguments(args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wrapper = storageManager.getStory(WrapperParcelable.<Story>parcelableToObject(getArguments().getParcelable(
                "story")));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleView.setOnClickListener(v -> {
            Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.loading_dialog);
            dialog.show();
            taskManager.execute(taskContext, () -> {
                try {
                    epubBuilder.openEpub(getActivity(), wrapper);
                } catch (Exception e) {
                    log.error("Failed to open epub", e);
                    toasts.toast("Failed to open epub", e);
                } finally {
                    getActivity().runOnUiThread(dialog::hide);
                }
            });
        });
        titleView.setOnLongClickListener(v -> {
            showDialog(new AsyncAction(R.string.open_in_browser, () -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(wrapper.getStory().getUri().toString()));
                getActivity().startActivity(intent);
            }));
            return false;
        });
        refresh();
    }

    private List<ChapterHolder> chapterHolders = new ArrayList<>();

    private void refresh() {
        titleView.setText(wrapper.getStory().getTitle());

        Author author = wrapper.getStory().getAuthor();
        if (author == null) {
            authorView.setVisibility(View.GONE);
        } else {
            authorView.setVisibility(View.VISIBLE);
            authorView.setText(author.getName());
        }

        AndroidFictionProvider provider = providerManager.getProvider(wrapper.getStory());
        tagsView.setText(StringUtils.join(provider.getTags(wrapper.getStory()), " • "));

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

    void refreshAsync() {
        getActivity().runOnUiThread(StoryFragment.this::refresh);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    void showDialog(AsyncAction... actions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] actionNames = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            actionNames[i] = getResources().getString(actions[i].description);
        }
        builder.setItems(actionNames, (dialog, which) -> {
            taskManager.execute(taskContext, actions[which].task);
        });
        builder.show();
    }

    private class ChapterHolder {
        private final View view;
        private final int index;

        private final CheckBox readBox;

        ChapterHolder(View view, int index) {
            this.view = view;
            this.index = index;

            readBox = (CheckBox) view.findViewById(R.id.chapterRead);
            readBox.setOnCheckedChangeListener((buttonView, isChecked) -> setRead(isChecked));
            readBox.setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.read_until_here, () -> {
                    for (int i = 0; i <= index && i < chapterHolders.size(); i++) {
                        chapterHolders.get(i).setRead(true);
                    }
                    refreshAsync();
                }));
                return true;
            });

            View.OnClickListener refreshListener = v -> fetchChapter(false);
            view.findViewById(R.id.chapterDownload).setOnClickListener(refreshListener);
            view.findViewById(R.id.chapterDownload).setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.download_until_here, () -> {
                    for (int i = 0; i <= index && i < chapterHolders.size(); i++) {
                        ChapterHolder holder = chapterHolders.get(i);
                        // don't redownload
                        if (!holder.hasText()) {
                            holder.fetchChapter(true);
                        }
                    }
                    refreshAsync();
                }));
                return true;
            });
            view.findViewById(R.id.chapterRefresh).setOnClickListener(refreshListener);
            view.findViewById(R.id.chapterRefresh).setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.refresh_until_here, () -> {
                    for (int i = 0; i <= index && i < chapterHolders.size(); i++) {
                        chapterHolders.get(i).fetchChapter(true);
                    }
                    refreshAsync();
                }));
                return true;
            });
        }

        void setRead(boolean read) {
            wrapper.setChapterRead(index, read);
        }

        void setChapter(Chapter chapter) {
            String name = chapter.getName();
            if (name == null) {
                name = "Chapter " + (index + 1);
            }
            ((TextView) view.findViewById(R.id.chapterName)).setText(name);

            boolean hasText = hasText();

            setChapterViewStatus(hasText ? R.id.chapterRefresh : R.id.chapterDownload);

            readBox.setVisibility(hasText ? View.VISIBLE : View.INVISIBLE);
            readBox.setChecked(wrapper.isChapterRead(index));
        }

        boolean hasText() {
            return wrapper.hasChapterText(index);
        }

        void fetchChapter(boolean sync) {
            getActivity().runOnUiThread(() -> setChapterViewStatus(R.id.chapterDownloading));

            Story storyClone = pojoMerger.clone(wrapper.getStory());
            Chapter chapter = storyClone.getChapters().get(index);
            AndroidFictionProvider provider = providerManager.getProvider(storyClone);

            Runnable task = () -> {
                try {
                    provider.fetchChapter(storyClone, chapter);
                    storageManager.mergeStory(storyClone);
                    refreshAsync();
                } catch (Exception e) {
                    log.error("Failed to fetch chapter", e);
                    toasts.toast("Failed to fetch chapter", e);
                }
            };
            if (sync) {
                task.run();
            } else {
                taskManager.execute(taskContext, task);
            }
        }

        private void setChapterViewStatus(int statusId) {
            ViewGroup group = (ViewGroup) view.findViewById(R.id.downloadWrapper);
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                child.setVisibility(child.getId() == statusId ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    @RequiredArgsConstructor
    private static class AsyncAction {
        final int description;
        final Runnable task;
    }
}
