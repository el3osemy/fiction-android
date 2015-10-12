package at.yawk.fiction.android.storage;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.support.DatabaseResults;
import java.sql.SQLException;

/**
 * @author yawkat
 */
abstract class TransformedCloseableIterator<S, M> implements CloseableIterator<M> {
    private final CloseableIterator<S> handle;

    public TransformedCloseableIterator(CloseableIterator<S> handle) {
        this.handle = handle;
    }

    @Override
    public void close() throws SQLException {
        handle.close();
    }

    @Override
    public void closeQuietly() {
        handle.closeQuietly();
    }

    @Override
    public DatabaseResults getRawResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToNext() {
        handle.moveToNext();
    }

    @Override
    public M first() throws SQLException {
        return map(handle.first());
    }

    @Override
    public M previous() throws SQLException {
        return map(handle.previous());
    }

    @Override
    public M current() throws SQLException {
        return map(handle.current());
    }

    @Override
    public M nextThrow() throws SQLException {
        return map(handle.nextThrow());
    }

    @Override
    public M moveRelative(int offset) throws SQLException {
        return map(handle.moveRelative(offset));
    }

    @Override
    public boolean hasNext() {
        return handle.hasNext();
    }

    @Override
    public M next() {
        return map(handle.next());
    }

    @Override
    public void remove() {
        handle.remove();
    }

    protected abstract M map(S entry);
}
