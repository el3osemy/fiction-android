package at.yawk.fiction.android.context;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WrapperParcelable implements Parcelable {
    private final Object value;

    public static final Creator<WrapperParcelable> CREATOR = new Creator<WrapperParcelable>() {
        @Override
        public WrapperParcelable createFromParcel(Parcel source) {
            String className = source.readString();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            byte[] array = source.createByteArray();
            Object value;
            try {
                value = ObjectMapperProvider.getObjectMapper().readValue(array, clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new WrapperParcelable(value);
        }

        @Override
        public WrapperParcelable[] newArray(int size) {
            return new WrapperParcelable[size];
        }
    };

    public static Parcelable objectToParcelable(Object o) {
        return new WrapperParcelable(o);
    }

    @SuppressWarnings("unchecked")
    public static <T> T parcelableToObject(Parcelable parcelable) {
        return (T) ((WrapperParcelable) parcelable).getValue();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        PublicByteArrayOutputStream bos = new PublicByteArrayOutputStream();
        try {
            ObjectMapperProvider.getObjectMapper().writeValue(bos, value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dest.writeString(value.getClass().getName());
        dest.writeByteArray(bos.getBuf(), 0, bos.size());
    }

}
