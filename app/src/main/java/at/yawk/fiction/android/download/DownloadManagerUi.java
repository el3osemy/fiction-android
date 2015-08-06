package at.yawk.fiction.android.download;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.event.Subscribe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yawkat
 */
@Singleton
public class DownloadManagerUi {
    @Inject Application application;
    @Inject DownloadManager metrics;

    private final AtomicInteger nextId = new AtomicInteger();

    private final Map<DownloadManagerMetrics.Task, Integer> shownNotificationIds = new HashMap<>();

    @Inject
    public DownloadManagerUi(EventBus eventBus) {
        eventBus.addWeakListeners(this);
    }

    @Subscribe
    public void managerUpdate(ManagerUpdateEvent event) {
        List<DownloadManagerMetrics.Task> tasks = metrics.getTasks();
        for (DownloadManagerMetrics.Task task : tasks) {
            buildNotification(tasks, task);
        }
    }

    @Subscribe
    public void taskUpdate(TaskUpdateEvent event) {
        buildNotification(metrics.getTasks(), event.getTask());
    }

    private void buildNotification(List<DownloadManagerMetrics.Task> allTasks, DownloadManagerMetrics.Task task) {
        boolean running = task.isRunning();

        int id;
        synchronized (this) {
            Integer idObj = shownNotificationIds.get(task);
            if (!running) {
                if (idObj == null) { return; }
                shownNotificationIds.remove(task);
            }

            if (idObj == null) {
                idObj = nextId.getAndIncrement();
                shownNotificationIds.put(task, idObj);
            }
            id = idObj;
        }

        NotificationManager notificationManager =
                (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);
        if (running) {
            long current = task.getCurrentProgress();
            long max = task.getMaxProgress();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
            builder.setSmallIcon(R.drawable.ic_file_download_white_24dp);
            builder.setContentTitle(task.getName());
            List<String> contentText = new ArrayList<>();
            if (max != -1) { contentText.add(current + "/" + max + " complete"); }
            if (allTasks.size() > 1) {
                int otherTaskCount = allTasks.size() - 1;
                contentText.add(otherTaskCount + " other task" + (otherTaskCount > 1 ? "s" : "") + " queued");
            }
            builder.setContentText(StringUtils.join(contentText, " • "));
            builder.setProgress((int) max, (int) current, max == -1);
            builder.setOngoing(true);
            Notification notification = builder.build();

            notificationManager.notify(id, notification);
        } else {
            notificationManager.cancel(id);
        }
    }
}