package at.yawk.fiction.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import butterknife.Bind;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@ContentView(R.layout.story_activity)
@ContentViewActivity.Dialog
public class StoryActivity extends ContentViewActivity {
    private static final String EXTRA_STORY_ID = "storyId";

    @Inject StoryManager storyManager;
    @Inject ProviderManager providerManager;
    @Inject TaskManager taskManager;
    @Inject Toasts toasts;

    private TaskContext taskContext = new TaskContext();

    @Bind(R.id.storyActivity) InterceptTouchFrameLayout storyActivity;

    public static Intent createLaunchIntent(Context context, StoryWrapper wrapper) {
        Intent intent = new Intent(context, StoryActivity.class);
        intent.putExtra(EXTRA_STORY_ID, wrapper.getId());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GestureDetector gestureDetector = new GestureDetector(this, new SlideDiscardGestureListener(this));
        storyActivity.setOnTouchInterceptedListener((v, event) -> gestureDetector.onTouchEvent(event));

        String id = getIntent().getStringExtra(EXTRA_STORY_ID);
        if (id != null) {
            open(storyManager.getStory(id));
            return;
        }

        Uri data = getIntent().getData();
        if (data != null) {
            for (AndroidFictionProvider provider : providerManager.getProviders()) {
                Story story = provider.getStory(data);
                if (story != null) {
                    StoryWrapper wrapper = storyManager.getStory(story);
                    if (wrapper.hasData()) {
                        open(wrapper);
                    } else {
                        taskManager.execute(taskContext, () -> {
                            try {
                                provider.fetchStory(story);
                                wrapper.updateStory(story);
                                open(wrapper);
                            } catch (Exception e) {
                                log.error("Failed to load story", e);
                                toasts.toast("Failed to load story", e);
                            }
                        });
                    }
                    return;
                }
            }

            toasts.toast("Could not parse uri {}", data);
            finish();
            return;
        }

        toasts.toast("No story info provided");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        taskContext.destroy();
    }

    private void open(StoryWrapper story) {
        StoryFragment fragment = new StoryFragment();
        fragment.setStory(story);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.storyActivity, fragment)
                .commit();
    }
}
