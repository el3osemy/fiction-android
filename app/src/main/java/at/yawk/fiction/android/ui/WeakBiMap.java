package at.yawk.fiction.android.ui;

import com.google.common.collect.MapMaker;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;

/**
 * BiMap-like class with weak keys and values.
 *
 * @author yawkat
 */
class WeakBiMap<K, V> {
    private static <K, V> Map<K, V> makeMap() {
        return new MapMaker()
                .weakKeys().weakValues()
                .makeMap();
    }

    private final Map<K, V> a = makeMap();
    private final Map<V, K> b = makeMap();

    @Nullable
    public K getByValue(V v) {
        return b.get(v);
    }

    @Nullable
    public V getByKey(K v) {
        return a.get(v);
    }

    public void put(@NonNull K key, @NonNull V value) {
        V oldValue = a.put(key, value);
        K oldKey = b.put(value, key);
        if (!key.equals(oldKey)) { a.remove(oldKey); }
        if (!value.equals(oldValue)) { b.remove(oldValue); }
    }
}
