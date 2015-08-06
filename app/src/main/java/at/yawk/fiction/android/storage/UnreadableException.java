package at.yawk.fiction.android.storage;

/**
 * Thrown when a stored object is unreadable by the current app configuration.
 *
 * @author yawkat
 */
public class UnreadableException extends Exception {
    public UnreadableException(Throwable throwable) {
        super(throwable);
    }
}
