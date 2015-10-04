package at.yawk.fiction.android.storage;

import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class Transaction {
    private final Runnable commitTask;

    private volatile int claims = 0;
    private volatile boolean requireSave = false;

    public synchronized void open() {
        claims++;
    }

    public synchronized void requestSave() {
        checkOpen();
        this.requireSave = true;
    }

    public void commit() {
        boolean runSave;
        synchronized (this) {
            checkOpen();
            runSave = --claims == 0 && this.requireSave;
            if (runSave) {
                this.requireSave = false;
            }
        }
        if (runSave) {
            commitTask.run();
        }
    }

    private void checkOpen() {
        if (claims <= 0) { throw new IllegalStateException("Transaction not open"); }
    }
}
