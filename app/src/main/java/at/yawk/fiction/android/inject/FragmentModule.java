package at.yawk.fiction.android.inject;

import android.app.Fragment;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author yawkat
 */
public class FragmentModule implements Module {
    final Fragment fragment;

    FragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Fragment.class).toInstance(fragment);
    }
}
