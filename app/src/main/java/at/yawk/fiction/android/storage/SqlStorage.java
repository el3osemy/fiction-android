package at.yawk.fiction.android.storage;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.google.common.collect.Iterators;
import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.BaseFieldConverter;
import com.j256.ormlite.stmt.Where;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.android.ContextHolder;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class SqlStorage {
    private final AndroidConnectionSource connectionSource;
    @Getter private final int migrationCount;

    @Inject
    public SqlStorage(Application application) {
        SQLiteDatabase database = application.openOrCreateDatabase("fiction", Context.MODE_PRIVATE, null);

        ContextHolder.setContext(application);

        Flyway flyway = new Flyway();
        flyway.setDataSource("jdbc:sqlite:" + database.getPath(), "", "");
        migrationCount = flyway.migrate();

        log.info("Migration count {}", migrationCount);

        connectionSource = new AndroidConnectionSource(database);
    }

    public <T, ID> Dao<T, ID> createDao(Class<T> type) throws SQLException {
        return DaoManager.createDao(connectionSource, type);
    }

    public static <T, ID> Where<T, ID> and(Where<T, ID> base, Collection<Where<T, ID>> clauses) {
        switch (clauses.size()) {
        case 0:
            return base.raw("1=0");
        case 1:
            return clauses.iterator().next();
        default:
            Iterator<Where<T, ID>> iterator = clauses.iterator();
            Where<T, ID> first = iterator.next();
            Where<T, ID> second = iterator.next();
            //noinspection unchecked
            return base.and(first, second, Iterators.toArray(iterator, Where.class));
        }
    }

    public static <T, ID> Where<T, ID> or(Where<T, ID> base, Collection<Where<T, ID>> clauses) {
        switch (clauses.size()) {
        case 0:
            return base.raw("1=1");
        case 1:
            return clauses.iterator().next();
        default:
            Iterator<Where<T, ID>> iterator = clauses.iterator();
            Where<T, ID> first = iterator.next();
            Where<T, ID> second = iterator.next();
            //noinspection unchecked
            return base.or(first, second, Iterators.toArray(iterator, Where.class));
        }
    }
}
