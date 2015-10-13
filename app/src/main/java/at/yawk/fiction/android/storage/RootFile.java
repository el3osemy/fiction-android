package at.yawk.fiction.android.storage;

import android.os.Environment;
import android.support.annotation.NonNull;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class RootFile {
    private final File root;

    @Inject
    RootFile() {
        root = makeRootDirectory();
    }

    @NonNull
    public static File makeRootDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "Fiction");
    }

    public File getRoot() {
        return root;
    }
}
