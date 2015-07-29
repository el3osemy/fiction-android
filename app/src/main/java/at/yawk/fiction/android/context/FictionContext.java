package at.yawk.fiction.android.context;

import android.app.Activity;
import android.app.Application;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import at.yawk.fiction.android.FictionApplication;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.StorageManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import lombok.Getter;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author yawkat
 */
@Getter
public class FictionContext {
    public static FictionContext get(Activity activity) {
        return ((FictionApplication) activity.getApplication()).getContext();
    }

    public static FictionContext get(Fragment fragment) {
        return get(fragment.getActivity());
    }

    private final Application application;
    private final ObjectMapper objectMapper;
    private final StorageManager storageManager;
    private final ProviderManager providerManager = new ProviderManager();
    private final TaskManager taskManager = new TaskManager();

    public FictionContext(Application application, File root) {
        this.application = application;
        this.objectMapper = ObjectMapperHolder.getObjectMapper();
        this.storageManager = new StorageManager(objectMapper, root);
    }

    public Parcelable objectToParcelable(Object o) {
        return new WrapperParcelable(o);
    }

    @SuppressWarnings("unchecked")
    public <T> T parcelableToObject(Parcelable parcelable) {
        return (T) ((WrapperParcelable) parcelable).getValue();
    }

    public void toast(Activity activity, String msg, Object... args) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(msg, args);
        String message = tuple.getMessage();

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Throwable thr = tuple.getThrowable();
        if (thr != null) {
            message += thr;
        }

        final String finalMessage = message;
        activity.runOnUiThread(() -> Toast.makeText(activity, finalMessage, Toast.LENGTH_SHORT).show());
    }
}
