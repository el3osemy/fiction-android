package at.yawk.fiction.android.inject;

/**
 * This is a terrible hack used to make dagger aware of a class that the {@link Injector} does not have access to at
 * compile time.
 *
 * @author yawkat
 */
public interface ExternalInjectable {
    /**
     * Get the module used to inject members of this injectable.
     */
    Object createModule();
}
