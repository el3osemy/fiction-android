package at.yawk.fiction.android.storage;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import java.sql.SQLException;
import lombok.Getter;
import org.joda.time.Instant;

/**
 * @author yawkat
 */
public class InstantPersister extends BaseDataType {
    @Getter private static final InstantPersister singleton = new InstantPersister();

    private InstantPersister() {
        super(SqlType.LONG, new Class[]{ Instant.class });
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        return new Instant(((Number) sqlArg).longValue());
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
        return ((Instant) javaObject).getMillis();
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getLong(columnPos);
    }
}
