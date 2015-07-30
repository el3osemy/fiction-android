package at.yawk.fiction.android;

import android.app.Application;
import android.os.Environment;
import at.yawk.fiction.android.context.FictionContext;
import java.io.File;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class FictionApplication extends Application {
    @Getter private FictionContext context;

    @Override
    public void onCreate() {
        super.onCreate();

        log.info("Starting application");
        log.debug("Creating context...");
        context = new FictionContext(this, new File(Environment.getExternalStorageDirectory(), "Fiction"));
        log.debug("Context created.");
    }
}
