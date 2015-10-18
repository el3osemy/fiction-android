package at.yawk.fiction.android;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import at.yawk.fiction.android.context.TaskContext;
import at.yawk.fiction.android.context.TaskManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class UpdateManager {
    @Inject Application application;
    @Inject ObjectMapper objectMapper;
    @Inject TaskManager taskManager;

    private final TaskContext taskContext = new TaskContext();

    @Getter
    @Nullable
    private Status lastKnownStatus;
    @Getter
    private boolean updatable;

    @Getter(lazy = true)
    private final String appBuild = loadAppBuild();

    private String loadAppBuild() {
        try {
            return new String(ByteStreams.toByteArray(application.getResources().openRawResource(R.raw.build)),
                              Charsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkUpdateAsync() {
        taskManager.execute(taskContext, () -> {
            try {
                checkUpdateSync();
            } catch (IOException e) {
                log.error("Failed to check for update", e);
            }
        });
    }

    private void checkUpdateSync() throws IOException {
        log.info("App build is {}", getAppBuild());

        if (getAppBuild().equals("dev")) {
            log.info("Skipping update check (dev)");
            return;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            log.warn("Not connected, skipping update check");
            updatable = false;
        }

        Status remoteRevision = downloadRevision();
        this.lastKnownStatus = remoteRevision;

        if (remoteRevision.getBuild().equals(appBuild)) {
            log.info("We are on latest version ({})", remoteRevision.getBuild());
            updatable = false;
            return;
        }

        log.info("An update is available: {}", remoteRevision);
        updatable = true;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(application, R.string.update_available_toast, Toast.LENGTH_LONG).show());
    }

    /**
     * Get the current revision status or {@code null} if the device is not connected.
     */
    private Status downloadRevision() throws IOException {
        URL url = new URL("http://ci.yawk.at/job/fiction-android/lastStableBuild/artifact/release.json");
        InputStream in = url.openStream();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            return objectMapper.readValue(in, Status.class);
        } finally {
            in.close();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String build;
        private URL download;
    }
}
