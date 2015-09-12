package at.yawk.fiction.android.inject;

import android.app.Fragment;
import at.yawk.fiction.android.ui.MainPreferenceFragment;
import dagger.Module;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Module(
        addsTo = ActivityModule.class,
        injects = {
                MainPreferenceFragment.class,
        }
)
public class FragmentModule {
    final Fragment fragment;

    FragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @SuppressWarnings("unused")
    @Deprecated
    FragmentModule() {
        throw new UnsupportedOperationException();
    }

    //@Provides
    @Singleton
    public Fragment fragment() {
        return fragment;
    }
}
