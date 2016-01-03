package at.yawk.fiction.android.inject;

import android.app.Activity;
import android.content.Context;
import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author yawkat
 */
public class ActivityModule implements Module {
    final Activity activity;

    ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Activity.class).toInstance(activity);
        binder.bind(Context.class).toInstance(activity);
    }
}
