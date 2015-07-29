package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;

/**
 * @author yawkat
 */
public class StoryActivity extends FragmentActivity implements ContextProvider {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.story_activity);

        Story story = getContext().parcelableToObject(getIntent().getParcelableExtra("story"));
        StoryFragment fragment = StoryFragment.create(getContext(), getContext().getStorageManager().getStory(story));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.storyActivity, fragment)
                .commit();
    }

    @Override
    public FictionContext getContext() {
        return FictionContext.get(this);
    }
}
