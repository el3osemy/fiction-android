package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.view.GestureDetector;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.storage.StorageManager;
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
    @Inject StorageManager storageManager;
    @Bind(R.id.storyActivity) InterceptTouchFrameLayout storyActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GestureDetector gestureDetector = new GestureDetector(this, new SlideDiscardGestureListener(this));
        storyActivity.setOnTouchInterceptedListener((v, event) -> gestureDetector.onTouchEvent(event));

        Story story = WrapperParcelable.parcelableToObject(getIntent().getParcelableExtra("story"));
        StoryFragment fragment = new StoryFragment();
        fragment.setStory(storageManager.getStory(story));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.storyActivity, fragment)
                .commit();
    }
}
