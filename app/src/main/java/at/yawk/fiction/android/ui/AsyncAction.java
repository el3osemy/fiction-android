package at.yawk.fiction.android.ui;

import android.content.Context;
import at.yawk.fiction.android.Consumer;
import at.yawk.fiction.android.context.FragmentUiRunner;
import at.yawk.fiction.android.context.TaskContext;
import lombok.Getter;
import lombok.Value;

/**
 * Asynchronous, named action, for example on a button.
 *
 * @author yawkat
 */
@Getter
public class AsyncAction {
    private final int description;
    private final Consumer<AsyncActionContext> task;

    public AsyncAction(int description, Runnable task) {
        this(description, ctx -> task.run());
    }

    public AsyncAction(int description, Consumer<AsyncActionContext> task) {
        this.description = description;
        this.task = task;
    }

    @Value
    public static final class AsyncActionContext {
        private final Context context;
        private final FragmentUiRunner uiRunner;
        private final TaskContext taskContext;
    }
}
