package at.yawk.fiction.android.event;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
public class EventBus {
    private static final Threader sameThreader = new Threader() {
        @Override
        public boolean isRunnableHere() {
            return true;
        }

        @Override
        public void runLater(Runnable runnable) {
            throw new UnsupportedOperationException();
        }
    };

    private final Map<Class<?>, HandlerList> handlerLists = new ConcurrentHashMap<>();

    private final ReferenceQueue<?> referenceQueue = new ReferenceQueue<>();

    private final Threader uiThreader;

    public EventBus(android.os.Handler handler) {
        uiThreader = new Threader() {
            @Override
            public boolean isRunnableHere() {
                return handler.getLooper().getThread() == Thread.currentThread();
            }

            @Override
            public void runLater(Runnable runnable) {
                handler.post(runnable);
            }
        };
    }

    public void post(Object event) {
        getHandlerList(event.getClass()).post(event);
    }

    /**
     * Add all defined listeners on the given class. Only a weak reference to the given handle will be kept. When the
     * handle is garbage collected, the listener will be discarded.
     */
    public void addWeakListeners(Object handle) {
        addListeners0(handle.getClass(), handle);
    }

    private void addListeners0(Class<?> declaring, Object handle) {
        for (Method method : declaring.getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                method.setAccessible(true);
                Threader threader = annotation.value() == Subscribe.EventQueue.UI ? uiThreader : sameThreader;
                addHandler(method.getParameterTypes()[0],
                           new RefHandler(new WeakReference<>(handle), method, threader));
            }
        }
        Class<?> superclass = declaring.getSuperclass();
        if (superclass != null) { addListeners0(superclass, handle); }
        for (Class<?> itf : declaring.getInterfaces()) {
            addListeners0(itf, handle);
        }
    }

    private void addHandler(Class<?> type, Handler handler) {
        getHandlerList(type).addHandler(handler);
    }

    HandlerList getHandlerList(Class<?> type) {
        HandlerList handlerList = handlerLists.get(type);
        if (handlerList == null) {
            synchronized (this) {
                handlerList = handlerLists.get(type);
                if (handlerList == null) {
                    handlerList = new HandlerList(type);
                    handlerList.addToSuper(handlerList);
                    handlerLists.put(type, handlerList);
                }
            }
        }
        return handlerList;
    }

    @RequiredArgsConstructor
    private class HandlerList {
        private final Class<?> type;
        private final Set<Handler> handlers = new CopyOnWriteArraySet<>();
        private final Set<HandlerList> subHandlers = new CopyOnWriteArraySet<>();

        void addSubList(HandlerList list) {
            if (subHandlers.add(list)) {
                addToSuper(list);

                list.handlers.addAll(this.handlers);
            }
        }

        void addToSuper(HandlerList list) {
            Class<?> sup = type.getSuperclass();
            if (sup != null) {
                getHandlerList(sup).addSubList(list);
            }
            for (Class<?> itf : type.getInterfaces()) {
                getHandlerList(itf).addSubList(list);
            }
        }

        void addHandler(Handler handler) {
            handlers.add(handler);
            for (HandlerList subHandler : subHandlers) {
                subHandler.addHandler(handler);
            }
        }

        void post(Object event) {
            for (Iterator<Handler> iterator = handlers.iterator(); iterator.hasNext(); ) {
                Handler handler = iterator.next();
                if (!handler.accept(event)) {
                    iterator.remove();
                }
            }
        }
    }
}
