package at.yawk.fiction.android.inject;

import android.support.v4.app.Fragment;
import at.yawk.fiction.android.ui.DownloadManagerFragment;
import at.yawk.fiction.android.ui.QueryFragment;
import at.yawk.fiction.android.ui.StoryFragment;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Module(
        addsTo = ActivityModule.class,
        injects = {
                QueryFragment.class,
                StoryFragment.class,
                DownloadManagerFragment.class,
        }
)
public class SupportFragmentModule {
    final Fragment fragment;

    SupportFragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @SuppressWarnings("unused")
    @Deprecated
    SupportFragmentModule() {
        throw new UnsupportedOperationException();
    }

    @Provides
    @Singleton
    public Fragment fragment() {
        return fragment;
    }
}
