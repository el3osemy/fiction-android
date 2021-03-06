package at.yawk.fiction.android.context;

import android.app.Activity;
import android.widget.Toast;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author yawkat
 */
@Singleton
@AllArgsConstructor
public class Toasts {
    @Inject Activity activity;

    @Inject
    Toasts() {}

    public void toast(String msg, Object... args) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(msg, args);
        String message = tuple.getMessage();

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Throwable thr = tuple.getThrowable();
        if (thr != null) {
            message += thr;
        }

        final String finalMessage = message;
        this.activity.runOnUiThread(() -> Toast.makeText(this.activity, finalMessage, Toast.LENGTH_SHORT).show());
    }
}
