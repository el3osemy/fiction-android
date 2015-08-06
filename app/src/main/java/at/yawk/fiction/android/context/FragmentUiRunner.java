package at.yawk.fiction.android.context;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class FragmentUiRunner {
    @Inject Fragment fragment;

    public boolean runOnUiThread(Runnable task) {
        FragmentActivity activity = fragment.getActivity();
        if (activity != null) {
            activity.runOnUiThread(task);
            return true;
        } else {
            return false;
        }
    }
}
