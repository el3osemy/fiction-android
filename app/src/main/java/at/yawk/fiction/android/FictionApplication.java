package at.yawk.fiction.android;

import android.app.Application;
import android.os.Environment;
import at.yawk.fiction.android.context.FictionContext;
import java.io.File;
import lombok.Getter;

/**
 * @author yawkat
 */
public class FictionApplication extends Application {
    @Getter private FictionContext context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = new FictionContext(this, new File(Environment.getExternalStorageDirectory(), "Fiction"));
    }
}
