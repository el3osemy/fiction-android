package at.yawk.fiction.android.storage;

import com.google.common.base.Supplier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import lombok.SneakyThrows;
import org.joda.time.Instant;

/**
 * @author yawkat
 */
@Singleton
public class PojoMerger {
    private final ConcurrentMap<Class<?>, Merger<?>> mergers = new ConcurrentHashMap<>();

    {
        mergers.put(String.class, IDENTITY);
        mergers.put(URI.class, IDENTITY);
        mergers.put(Instant.class, IDENTITY);
        mergers.put(List.class, new CollectionMerger<>(ArrayList::new));
        mergers.put(Set.class, new CollectionMerger<>(HashSet::new));
    }

    @SuppressWarnings("unchecked")
    private <T> Merger<T> getMerger(Class<T> type) {
        if (type.isPrimitive()) { return (Merger<T>) IDENTITY; }

        Merger<?> present = mergers.get(type);
        if (present == null) {
            if (type.getName().startsWith("at.yawk.")) {
                present = Modifier.isAbstract(type.getModifiers()) ? ABSTRACT_MERGER : new ObjectMerger<>(type);
            } else {
                throw new Error("Cannot create merger for type " + type.getName());
            }

            Merger<?> concurrent = mergers.putIfAbsent(type, present);
            if (concurrent != null) { present = concurrent; }
        }
        return (Merger<T>) present;
    }

    @SuppressWarnings("unchecked")
    public <T> T clone(T obj) {
        return clone((Class<T>) obj.getClass(), obj);
    }

    private <T> T clone(Class<T> type, T obj) {
        return getMerger(type).clone(this, obj);
    }

    /**
     * Merge two pojos. Values from the first parameter will take priority.
     */
    @SuppressWarnings("unchecked")
    public <T> T merge(T a, T b) {
        return merge((Class<T>) a.getClass(), a, b);
    }

    private <T> T merge(Class<T> type, T a, T b) {
        return getMerger(type).merge(this, a, b);
    }

    private interface Merger<T> {
        T clone(PojoMerger merger, T obj);

        T merge(PojoMerger merger, T a, T b);
    }

    private static final Merger<Object> IDENTITY = new Merger<Object>() {
        @Override
        public Object clone(PojoMerger merger, Object obj) {
            return obj;
        }

        @Override
        public Object merge(PojoMerger merger, Object a, Object b) {
            return a;
        }
    };

    private static class ObjectMerger<T> implements Merger<T> {
        private final List<Field> fields;
        private final Constructor<T> constructor;

        private static List<Field> getFields(Class<?> cl) {
            List<Field> target = new ArrayList<>();
            do {
                Collections.addAll(target, cl.getDeclaredFields());
                cl = cl.getSuperclass();
            } while (cl != null);
            return target;
        }

        private ObjectMerger(Class<T> cl) {
            this.fields = getFields(cl);
            for (Iterator<Field> iterator = fields.iterator(); iterator.hasNext(); ) {
                Field field = iterator.next();
                if (Modifier.isStatic(field.getModifiers()) |
                    Modifier.isTransient(field.getModifiers())) {
                    iterator.remove();
                    continue;
                }

                field.setAccessible(true);
            }

            try {
                constructor = cl.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new Error("No suitable constructor for " + cl.getName(), e);
            }
            constructor.setAccessible(true);
        }

        @SuppressWarnings("unchecked")
        @Override
        @SneakyThrows
        public T clone(PojoMerger merger, T obj) {
            T clone = constructor.newInstance();
            for (Field field : fields) {
                Object value = field.get(obj);
                field.set(clone, value == null ? null : merger.clone((Class) field.getType(), value));
            }
            return clone;
        }

        @SuppressWarnings("unchecked")
        @Override
        @SneakyThrows
        public T merge(PojoMerger merger, T a, T b) {
            T clone = constructor.newInstance();
            for (Field field : fields) {
                Object va = field.get(a);
                Object vb = field.get(b);
                Object merged = va;
                if (vb != null) {
                    if (va == null) {
                        merged = vb;
                    } else if (!va.equals(vb)) {
                        merged = merger.merge((Class) field.getType(), va, vb);
                    }
                }
                field.set(clone, merged);
            }
            return clone;
        }
    }

    private static class CollectionMerger<E, T extends Collection<E>> implements Merger<T> {
        private final Supplier<T> factory;

        private CollectionMerger(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        @SneakyThrows
        public T clone(PojoMerger merger, T obj) {
            T clone = factory.get();
            clone.addAll(obj);
            return clone;
        }

        @Override
        @SneakyThrows
        public T merge(PojoMerger merger, T a, T b) {
            T clone = factory.get();
            Iterator<E> ai = a.iterator();
            Iterator<E> bi = b.iterator();
            while (ai.hasNext()) {
                E e = ai.next();
                if (bi.hasNext()) {
                    e = merger.merge(e, bi.next());
                }
                clone.add(e);
            }
            return clone;
        }
    }

    private static final Merger<Object> ABSTRACT_MERGER = new Merger<Object>() {
        @SuppressWarnings("unchecked")
        @Override
        public Object clone(PojoMerger merger, Object obj) {
            return merger.clone((Class) obj.getClass(), obj);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object merge(PojoMerger merger, Object a, Object b) {
            Class<?> cl = a.getClass();
            if (!cl.isInstance(b)) {
                return a;
            } else {
                return merger.merge((Class) cl, a, b);
            }
        }
    };
}
