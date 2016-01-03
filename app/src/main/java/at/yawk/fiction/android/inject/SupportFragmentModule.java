package at.yawk.fiction.android.inject;

import android.support.v4.app.Fragment;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author yawkat
 */
public class SupportFragmentModule implements Module {
    final Fragment fragment;

    SupportFragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Fragment.class).toInstance(fragment);
    }
}
