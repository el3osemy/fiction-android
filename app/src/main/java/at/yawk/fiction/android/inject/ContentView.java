package at.yawk.fiction.android.inject;

import java.lang.annotation.*;

/**
 * @author yawkat
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ContentView {
    int value();
}
