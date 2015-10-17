package at.yawk.fiction.android;

import android.app.Application;
import at.yawk.fiction.android.inject.Injector;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class FictionApplication extends Application {
    @Inject UpdateManager updateManager;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Fabric.with(this, new Crashlytics());

            Injector.init(this);
            Injector.inject(this);

            updateManager.checkUpdateAsync();
        } catch (Throwable t) {
            log.error("Failed to initialize", t);
            throw t;
        }
    }
}
