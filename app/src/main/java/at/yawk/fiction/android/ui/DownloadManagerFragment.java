package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.download.DownloadManager;
import at.yawk.fiction.android.download.DownloadManagerMetrics;
import at.yawk.fiction.android.download.ManagerUpdateEvent;
import at.yawk.fiction.android.download.TaskUpdateEvent;
import at.yawk.fiction.android.event.Subscribe;
import at.yawk.fiction.android.inject.ContentView;
import butterknife.Bind;
import butterknife.ButterKnife;
import java.util.ArrayList;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@ContentView(R.layout.download_list)
public class DownloadManagerFragment extends ContentViewFragment {
    @Inject DownloadManager downloadManager;

    @Bind(R.id.downloadList) ListView downloadList;
    @Bind(R.id.noDownloads) View noDownloads;

    SimpleArrayAdapter<DownloadManagerMetrics.Task> adapter;

    private final WeakBiMap<DownloadManagerMetrics.Task, View> decoratorMap = new WeakBiMap<>();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new SimpleArrayAdapter<DownloadManagerMetrics.Task>(
                getActivity(), R.layout.download_list_entry, new ArrayList<>()) {
            @Override
            protected void decorateView(View view, int position) {
                DownloadManagerMetrics.Task task = getItem(position);
                decoratorMap.put(task, view);
                new TaskDecorator(view).decorate(task);
            }
        };
        downloadList.setAdapter(adapter);
        downloadList.setEmptyView(noDownloads);

        updateAll();
    }

    @Subscribe(Subscribe.EventQueue.UI)
    public void taskUpdate(TaskUpdateEvent event) {
        DownloadManagerMetrics.Task task = event.getTask();
        View view = decoratorMap.getByKey(task);
        if (view != null) {
            new TaskDecorator(view).decorate(task);
        }
    }

    @Subscribe(Subscribe.EventQueue.UI)
    public void managerUpdate(ManagerUpdateEvent event) {
        updateAll();
    }

    private void updateAll() {
        adapter.clear();
        adapter.addAll(downloadManager.getTasks());
    }

    @RequiredArgsConstructor
    static class TaskDecorator {
        private final View view;

        @Bind(R.id.taskName) TextView taskName;
        @Bind(R.id.taskProgressText) TextView taskProgressText;
        @Bind(R.id.taskProgress) ProgressBar taskProgress;
        @Bind(R.id.cancelTask) View cancelTask;

        void decorate(DownloadManagerMetrics.Task task) {
            ButterKnife.bind(this, view);

            taskName.setText(task.getName());

            long current = task.getCurrentProgress();
            long max = task.getMaxProgress();

            if (max == -1) {
                taskProgressText.setVisibility(View.GONE);

                taskProgress.setIndeterminate(true);
            } else {
                taskProgressText.setVisibility(View.VISIBLE);
                taskProgressText.setText(current + "/" + max + " complete");

                taskProgress.setIndeterminate(false);
                taskProgress.setProgress((int) current);
                taskProgress.setMax((int) max);
            }

            cancelTask.setOnClickListener(v -> task.cancel());
        }
    }
}
