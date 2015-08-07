package at.yawk.fiction.android.ui;

import android.os.Bundle;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.inject.ContentView;
import at.yawk.fiction.android.storage.StorageManager;
import javax.inject.Inject;

/**
 * @author yawkat
 */
@ContentView(R.layout.story_activity)
@ContentViewActivity.Dialog
public class StoryActivity extends ContentViewActivity {
    @Inject StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Story story = WrapperParcelable.parcelableToObject(getIntent().getParcelableExtra("story"));
        StoryFragment fragment = new StoryFragment();
        fragment.setStory(storageManager.getStory(story));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.storyActivity, fragment)
                .commit();
    }
}
