package at.yawk.fiction.android.event;

/**
 * @author yawkat
 */
interface Threader {
    boolean isRunnableHere();

    void runLater(Runnable runnable);
}
