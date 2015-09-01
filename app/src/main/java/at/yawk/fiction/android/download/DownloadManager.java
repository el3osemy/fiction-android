package at.yawk.fiction.android.download;

import at.yawk.fiction.android.download.task.*;
import at.yawk.fiction.android.event.EventBus;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Singleton
@Slf4j
public class DownloadManager implements DownloadManagerMetrics {
    private final Map<Class<? extends DownloadTask>, DownloadTaskHandler<?>> handlers = new HashMap<>();
    @Inject EventBus bus;

    /**
     * All currently queued tasks, including the currently running task.
     */
    private final Collection<Task> tasks = Collections.synchronizedCollection(new ArrayDeque<>());
    @Getter(AccessLevel.PACKAGE)
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Inject
    DownloadManager(
            ChapterDownloadTaskHandler chapterDownloadTaskHandler,
            StoryUpdateTaskHandler storyUpdateTaskHandler
    ) {
        addHandler(ChapterDownloadTask.class, chapterDownloadTaskHandler);
        addHandler(StoryUpdateTask.class, storyUpdateTaskHandler);
    }

    private <T extends DownloadTask> void addHandler(Class<T> type, DownloadTaskHandler<T> handler) {
        handlers.put(type, handler);
    }

    public void enqueue(DownloadTask task) {
        AbstractTask<? super DownloadTask> wrapper = createTask(task);
        tasks.add(wrapper);
        wrapper.addCompletionListener(() -> {
            tasks.remove(wrapper);
            fireManagerUpdateEvent();
        });
        wrapper.schedule();
        fireManagerUpdateEvent();
    }

    private void fireManagerUpdateEvent() {
        bus.post(new ManagerUpdateEvent());
    }

    @Override
    public Collection<Task> getTasks() {
        return Collections.unmodifiableCollection(tasks);
    }

    @SuppressWarnings("unchecked")
    <T extends DownloadTask> AbstractTask<? super T> createTask(T task) {
        DownloadTaskHandler<T> handler = (DownloadTaskHandler<T>) handlers.get(task.getClass());
        if (handler != null) {
            return new SimpleTask<>(this, task, handler);
        }
        if (task instanceof SplittableDownloadTask) {
            return (AbstractTask<? super T>) new SplitTask(this, (SplittableDownloadTask) task);
        }
        throw new UnsupportedOperationException("No handler for task " + task.getClass().getName());
    }
}
