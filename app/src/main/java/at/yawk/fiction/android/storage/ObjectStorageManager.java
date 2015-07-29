package at.yawk.fiction.android.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ObjectStorageManager {
    static final Joiner SLASH_JOINER = Joiner.on('/');

    private final File root;
    private final ObjectMapper objectMapper;

    public <T> T load(Class<T> type, String key) throws NotFoundException {
        File file = new File(root, key);
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NotFoundException();
        }
    }

    public void save(Object o, String key) {
        File file = new File(root, key);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        try {
            objectMapper.writeValue(file, o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(String key) {
        return new File(root, key).exists();
    }
}
