package at.yawk.fiction.android;

import android.support.multidex.MultiDexApplication;
import at.yawk.fiction.android.inject.Injector;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.impl.AndroidLoggingProviderImpl;

/**
 * @author yawkat
 */
@Slf4j
public class FictionApplication extends MultiDexApplication {
    @Inject UpdateManager updateManager;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidLoggingProviderImpl.init(this);

        try {
            log.info("Starting up...");
            Fabric.with(this, new Crashlytics());

            log.info("Setting up injector...");
            Injector.init(this);
            Injector.inject(this);

            updateManager.checkUpdateAsync();
            log.info("Startup complete!");
        } catch (Throwable t) {
            log.error("Failed to initialize", t);
            throw t;
        }
    }
}
