package at.yawk.fiction.android.storage;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import at.yawk.fiction.android.Consumer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class RootFile {
    private static final File root = new File(Environment.getExternalStorageDirectory(), "Fiction");
    private static final Collection<Consumer<File>> callbacks = new ArrayList<>();

    private static boolean hasPermission = false;
    private Application application;

    @Inject
    RootFile(Application application) {
        this.application = application;
    }

    public static synchronized void getRoot(Context context, Consumer<File> callback) {
        if (hasPermission(context)) {
            callback.consume(root);
        } else {
            callbacks.add(callback);
        }

    }

    public static synchronized boolean hasPermission(Context context) {
        if (!hasPermission) {
            hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (hasPermission) {
                Log.i("", "We were granted WRITE_EXTERNAL_STORAGE permission.");
                for (Consumer<File> callback : callbacks) {
                    try {
                        callback.consume(root);
                    } catch (Throwable t) {
                        log.error("Failed to execute root file callback", t);
                    }
                }
                callbacks.clear();
            }
        }
        return hasPermission;
    }

    public void getRoot(Consumer<File> consumer) {
        getRoot(application, consumer);
    }

    public synchronized File getRootNow() throws IllegalStateException {
        if (!hasPermission()) throw new IllegalStateException("No permission to write to external storage");
        return root;
    }

    public boolean hasPermission() {
        return hasPermission(application);
    }
}
