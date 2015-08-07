package at.yawk.fiction.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import at.yawk.fiction.android.inject.Injector;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class ContentViewActivity extends FragmentActivity {
    @Inject SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.injectActivity(this);

        // choice settings can only be strings: https://code.google.com/p/android/issues/detail?id=2096
        setTheme(Theme.values()[Integer.parseInt(preferences.getString("theme", "0"))].getTheme());

        setContentView(Injector.buildAndInjectContentView(this, getLayoutInflater(), null));
    }
}
