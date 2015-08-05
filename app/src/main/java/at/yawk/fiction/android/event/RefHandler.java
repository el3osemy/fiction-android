package at.yawk.fiction.android.event;

import java.lang.ref.Reference;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * An event handler that invokes a method on an object that is stored in an arbitrary reference (for example weak
 * reference).
 *
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
final class RefHandler implements Handler {
    private final Reference<?> ref;
    private final Method method;
    private final Threader threader;

    @Override
    public boolean accept(Object event) {
        Object o = ref.get();
        if (o == null) { return false; }

        if (threader.isRunnableHere()) {
            accept0(o, event);
        } else {
            threader.runLater(() -> accept0(o, event));
        }
        return true;
    }

    private void accept0(Object o, Object event) {
        try {
            method.invoke(o, event);
        } catch (Throwable e) {
            log.error("Failed to invoke event handler {}", method, e);
        }
    }
}
