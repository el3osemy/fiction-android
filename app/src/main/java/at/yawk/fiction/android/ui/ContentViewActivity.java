package at.yawk.fiction.android.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.Injector;
import java.lang.annotation.*;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class ContentViewActivity extends AppCompatActivity {
    private static final int[] THEME_IDS_NORMAL = {
            R.style.FActionBar_Dark,
            R.style.FActionBar_Dark_Amoled,
    };
    private static final int[] THEME_IDS_NO_ACTION_BAR = {
            R.style.FNoActionBar_Dark,
            R.style.FNoActionBar_Dark_Amoled,
    };
    private static final int[] THEME_IDS_DIALOG = {
            R.style.FDialog_Dark,
            R.style.FDialog_Dark_Amoled,
    };

    @Inject SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inject();
    }

    protected void inject() {
        Injector.injectActivity(this);

        boolean dialog = getClass().isAnnotationPresent(Dialog.class);
        boolean noActionBar = getClass().isAnnotationPresent(NoActionBar.class);
        int[] themeSet;
        if (dialog) {
            themeSet = THEME_IDS_DIALOG;
        } else if (noActionBar) {
            themeSet = THEME_IDS_NO_ACTION_BAR;
        } else {
            themeSet = THEME_IDS_NORMAL;
        }

        // choice settings can only be strings: https://code.google.com/p/android/issues/detail?id=2096
        setTheme(themeSet[Integer.parseInt(preferences.getString("theme", "0"))]);

        setContentView(Injector.buildAndInjectContentView(this, getLayoutInflater(), null));
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    protected @interface Dialog {}

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    protected @interface NoActionBar {}
}
