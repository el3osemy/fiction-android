package at.yawk.fiction.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import at.yawk.fiction.*;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.*;
import at.yawk.fiction.android.download.DownloadManager;
import at.yawk.fiction.android.download.task.ChapterDownloadTask;
import at.yawk.fiction.android.download.task.ChapterRangeDownloadTask;
import at.yawk.fiction.android.event.StoryUpdateEvent;
import at.yawk.fiction.android.event.Subscribe;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.storage.EpubBuilder;
import at.yawk.fiction.android.storage.PojoMerger;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import butterknife.Bind;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.story)
public class StoryFragment extends ContentViewFragment {
    @Inject Toasts toasts;
    @Inject StoryManager storyManager;
    @Inject TaskManager taskManager;
    @Inject EpubBuilder epubBuilder;
    @Inject DownloadManager downloadManager;
    @Inject FragmentUiRunner uiRunner;
    @Inject PojoMerger merger;
    @Inject SharedPreferences sharedPreferences;

    private TaskContext taskContext = new TaskContext();
    private final Handler uiRunnerHandler = new Handler(Looper.getMainLooper());

    private StoryWrapper wrapper;

    @Bind(R.id.chapters) ViewGroup chapterGroup;
    @Bind(R.id.title) TextView titleView;
    @Bind(R.id.author) TextView authorView;
    @Bind(R.id.tags) TextView tagsView;
    @Bind(R.id.descriptionContainer) ViewGroup descriptionContainer;
    @Bind(R.id.description) TextView descriptionView;
    @Bind(R.id.coverImage) ImageView coverImage;
    @Bind(R.id.updateStory) Button updateStory;

    private int collapsedDescriptionHeight;

    public void setStory(StoryWrapper wrapper) {
        Bundle args = new Bundle();
        args.putParcelable("story", WrapperParcelable.objectToParcelable(wrapper.getStory()));
        setArguments(args);
    }

    @Subscribe(Subscribe.EventQueue.UI)
    public void onStoryUpdate(StoryUpdateEvent event) {
        if (event.getStory().equals(wrapper)) {
            refresh();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wrapper = storyManager.getStory(WrapperParcelable.<Story>parcelableToObject(getArguments().getParcelable(
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
                    uiRunner.runOnUiThread(dialog::hide);
                }
                wrapper.bumpLastActionTime();
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
        updateStory.setOnClickListener(v -> taskManager.execute(taskContext, () -> {
            Story story = merger.clone(wrapper.getStory());
            try {
                wrapper.getProvider().fetchStory(story);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            wrapper.updateStory(story);
        }));
        refresh();
    }

    private List<ChapterHolder> chapterHolders = new ArrayList<>();

    private void refresh() {
        Activity activity = getActivity();
        if (activity == null) {
            log.warn("No activity present in StoryFragment.refresh, skipping");
            return;
        }

        titleView.setText(wrapper.getStory().getTitle());

        Author author = wrapper.getStory().getAuthor();
        if (author == null) {
            authorView.setVisibility(View.GONE);
        } else {
            authorView.setVisibility(View.VISIBLE);
            authorView.setText(author.getName());
        }

        AndroidFictionProvider provider = wrapper.getProvider();
        tagsView.setText(StringUtils.join(provider.getTags(wrapper.getStory()), " • "));

        FormattedText description = wrapper.getStory().getDescription();
        if (description instanceof HtmlText) {
            URI uri = wrapper.getStory().getUri();
            Document document = Jsoup.parse(((HtmlText) description).getHtml(), uri == null ? "" : uri.toString());
            Cleaner cleaner = new Cleaner(Whitelist.basic());
            cleaner.clean(document);
            for (Element link : document.getElementsByTag("a")) {
                link.attr("href", link.absUrl("href"));
            }

            descriptionView.setText(Html.fromHtml(document.outerHtml()));
        } else if (description instanceof RawText) {
            descriptionView.setText(((RawText) description).getText());
        } else {
            descriptionView.setText("");
        }

        descriptionView.setMovementMethod(LinkMovementMethod.getInstance());

        collapsedDescriptionHeight = getMaxHeight(descriptionView);
        descriptionView.setOnClickListener(v -> setDescriptionCollapsed(!isDescriptionCollapsed()));

        coverImage.setVisibility(View.GONE);

        // when the image is loaded, adjust the min height of the description view to match it.
        coverImage.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = coverImage.getMeasuredHeight();
            if (collapsedDescriptionHeight < height) {
                collapsedDescriptionHeight = height;
                setDescriptionCollapsed(isDescriptionCollapsed());
            }
        });

        Image image = wrapper.getStory().getImage();
        if (image != null) {
            URI[] candidates = new URI[]{ image.getImageUrl(), image.getThumbnailUrl() };
            URI validUri = null;
            for (URI candidate : candidates) {
                if (isImageUriValid(candidate)) {
                    validUri = candidate;
                    break;
                }
            }

            if (validUri != null) {
                log.debug("Loading cover {}", validUri);

                boolean loadCoverViaNetwork = false;

                ConnectivityManager connectivityManager =
                        (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                log.info("network {}", activeNetworkInfo);
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                    log.info("network {}", activeNetworkInfo.getSubtype());
                    switch (activeNetworkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                    case ConnectivityManager.TYPE_ETHERNET:
                        loadCoverViaNetwork = sharedPreferences.getBoolean("download_cover.wifi", true);
                        break;
                    default:
                        loadCoverViaNetwork = sharedPreferences.getBoolean("download_cover.data", false);
                        break;
                    }
                }

                RequestCreator creator = Picasso.with(activity)
                        .load(Uri.decode(validUri.toString()));
                if (!loadCoverViaNetwork) {
                    creator = creator.networkPolicy(NetworkPolicy.OFFLINE);
                }
                creator.into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        coverImage.setVisibility(View.VISIBLE);
                        coverImage.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {}

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {}
                });
            }
        }

        List<? extends Chapter> chapters = wrapper.getStory().getChapters();
        if (chapters == null || chapters.isEmpty()) {
            updateStory.setVisibility(View.VISIBLE);
        } else {
            updateStory.setVisibility(View.GONE);

            if (chapters.size() < chapterHolders.size()) {
                List<ChapterHolder> toRemove = chapterHolders.subList(chapters.size(), chapterHolders.size());
                for (int i = 0; i < toRemove.size(); i++) {
                    chapterGroup.removeViewAt(chapters.size() + i);
                }
                toRemove.clear();
            }

            // spread out updates across multiple ticks - 10 at a time
            Runnable updateTask = new Runnable() {
                private int chapterIndex;

                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity == null) { return; }

                    int i = 0;
                    while (i++ < 10 && chapterIndex < chapters.size()) {

                        ChapterHolder holder;
                        if (chapterIndex >= chapterHolders.size()) {
                            // inflate attaches us to the group as well
                            View view = activity.getLayoutInflater().inflate(R.layout.chapter, chapterGroup, false);
                            chapterGroup.addView(view);
                            chapterHolders.add(holder = new ChapterHolder(view, chapterIndex));
                        } else {
                            holder = chapterHolders.get(chapterIndex);
                        }
                        holder.setChapter(chapters.get(chapterIndex));

                        chapterIndex++;
                    }

                    // reschedule
                    uiRunnerHandler.postDelayed(this, 10);
                }
            };

            updateTask.run();
        }
    }

    private boolean isDescriptionCollapsed() {
        return getMaxHeight(descriptionView) != Integer.MAX_VALUE;
    }

    private void setDescriptionCollapsed(boolean collapsed) {
        descriptionView.setMaxHeight(collapsed ? collapsedDescriptionHeight : Integer.MAX_VALUE);
    }

    private static boolean isImageUriValid(URI imageUri) {
        return imageUri != null &&
               imageUri.getScheme() != null &&
               imageUri.getScheme().matches("https?");
    }

    private static int getMaxHeight(TextView descriptionView) {
        if (Build.VERSION.SDK_INT < 16) {
            return 0;
        } else {
            return descriptionView.getMaxHeight();
        }
    }

    void refreshAsync() {
        uiRunner.runOnUiThread(StoryFragment.this::refresh);
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
            readBox.setOnClickListener(buttonView -> taskManager.execute(taskContext, () -> {
                try {
                    setRead(readBox.isChecked());
                } catch (Exception e) {
                    log.warn("Failed to set chapter read", e);
                    toasts.toast("Failed to set chapter read", e);
                }
            }));
            readBox.setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.read_until_here, () -> {
                    try {
                        for (int i = 0; i <= index && i < chapterHolders.size(); i++) {
                            chapterHolders.get(i).setRead(true);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to set chapters read", e);
                        toasts.toast("Failed to set chapters read", e);
                    }
                    refreshAsync();
                }));
                return true;
            });

            View.OnClickListener refreshListener = v ->
                    downloadManager.enqueue(new ChapterDownloadTask(wrapper, this.index));
            view.findViewById(R.id.chapterDownload).setOnClickListener(refreshListener);
            view.findViewById(R.id.chapterDownload).setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.download_until_here, () ->
                        downloadManager.enqueue(new ChapterRangeDownloadTask(wrapper, 0, index + 1, true))));
                return true;
            });
            view.findViewById(R.id.chapterRefresh).setOnClickListener(refreshListener);
            view.findViewById(R.id.chapterRefresh).setOnLongClickListener(v -> {
                showDialog(new AsyncAction(R.string.refresh_until_here, () ->
                        downloadManager.enqueue(new ChapterRangeDownloadTask(wrapper, 0, index + 1, false))));
                return true;
            });
        }

        /**
         * Blocking operation!
         */
        void setRead(boolean read) throws Exception {
            wrapper.setChapterRead(index, read);
        }

        void setChapter(Chapter chapter) {
            String name = chapter.getName();
            if (name == null) {
                name = "Chapter " + (index + 1);
            }
            ((TextView) view.findViewById(R.id.chapterName)).setText(name);

            boolean hasText = hasText();
            boolean downloading = wrapper.isDownloading(index);

            setChapterViewStatus(downloading ? R.id.chapterDownloading :
                                         (hasText ? R.id.chapterRefresh : R.id.chapterDownload));

            Boolean read = wrapper.isChapterRead(index);
            if (read != null) {
                readBox.setVisibility(View.VISIBLE);
                readBox.setChecked(read);
            } else {
                readBox.setVisibility(View.INVISIBLE);
            }
        }

        boolean hasText() {
            return wrapper.isChapterDownloaded(index);
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
