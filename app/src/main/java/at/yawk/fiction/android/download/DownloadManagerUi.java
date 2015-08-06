package at.yawk.fiction.android.download;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.event.EventBus;
import at.yawk.fiction.android.event.Subscribe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

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
    public void taskUpdate(TaskUpdateEvent event) {
        buildNotification(event.getTask());
    }

    private void buildNotification(DownloadManagerMetrics.Task task) {
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
            if (max != -1) {
                builder.setContentText(current + "/" + max + " complete");
            }
            builder.setProgress((int) max, (int) current, max == -1);
            builder.setOngoing(true);
            Notification notification = builder.build();

            notificationManager.notify(id, notification);
        } else {
            notificationManager.cancel(id);
        }
    }
}
