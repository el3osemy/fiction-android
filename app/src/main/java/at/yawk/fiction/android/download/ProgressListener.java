package at.yawk.fiction.android.download;

/**
 * @author yawkat
 */
public interface ProgressListener {
    ProgressListener NOOP = new ProgressListener() {
        @Override
        public ProgressListener createSubLevel() {
            return NOOP;
        }

        @Override
        public void progressDeterminate(long progress, long limit) {}

        @Override
        public void progressIndeterminate(boolean complete) {}
    };

    /**
     * Create a new progress sub-level. Note that multiple sub-levels may exist and be used concurrently.
     */
    ProgressListener createSubLevel();

    /**
     * Publish a new progress report with a known upper bound.
     */
    void progressDeterminate(long progress, long limit);

    /**
     * Publish a new progress report without known progress indication.
     *
     * @param complete <code>true</code> if this task was completed.
     */
    void progressIndeterminate(boolean complete);
}
