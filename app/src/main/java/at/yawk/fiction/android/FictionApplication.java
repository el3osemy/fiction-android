package at.yawk.fiction.android;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class FictionApplication extends Application {
    public static ApplicationInfo applicationInfo; // hack

    @Override
    public void onCreate() {
        super.onCreate();

        applicationInfo = getApplicationInfo();
    }
}
