package at.yawk.fiction.android.event;

/**
 * An event handler.
 *
 * @author yawkat
 */
interface Handler {
    /**
     * @return <code>true</code> if this handler is still valid, <code>false</code> if it should be removed.
     */
    boolean accept(Object event);
}
