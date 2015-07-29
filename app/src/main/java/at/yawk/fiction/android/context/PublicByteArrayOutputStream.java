package at.yawk.fiction.android.context;

import java.io.ByteArrayOutputStream;

/**
 * @author yawkat
 */
class PublicByteArrayOutputStream extends ByteArrayOutputStream {
    public byte[] getBuf() {
        return buf;
    }
}
