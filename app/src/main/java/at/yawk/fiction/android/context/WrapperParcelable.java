package at.yawk.fiction.android.context;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.IOException;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
class WrapperParcelable implements Parcelable {
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
                value = ObjectMapperHolder.getObjectMapper().readValue(array, clazz);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        PublicByteArrayOutputStream bos = new PublicByteArrayOutputStream();
        try {
            ObjectMapperHolder.getObjectMapper().writeValue(bos, value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dest.writeByteArray(bos.getBuf(), 0, bos.size());
    }

}
