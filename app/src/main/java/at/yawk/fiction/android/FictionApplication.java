package at.yawk.fiction.android;

import android.app.Application;
import at.yawk.fiction.android.inject.Injector;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class FictionApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Injector.init(this);
    }
}
