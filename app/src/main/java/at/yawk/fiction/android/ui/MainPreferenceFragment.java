package at.yawk.fiction.android.ui;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.UpdateManager;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.context.Toasts;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.storage.Index;
import java.io.File;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class MainPreferenceFragment extends PreferenceFragment {
    @Inject ProviderManager providerManager;
    @Inject UpdateManager updateManager;
    @Inject Toasts toasts;
    @Inject TaskManager taskManager;
    @Inject Index index;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.injectFragment(this);
        addPreferencesFromResource(R.xml.main);

        for (AndroidFictionProvider provider : providerManager.getProviders()) {
            Preference preference = provider.inflatePreference(getActivity(), getPreferenceManager());
            if (preference != null) {
                getPreferenceScreen().addPreference(preference);
            }
        }

        Preference updatePreference = getPreferenceManager().findPreference("update");
        if (updateManager.isUpdatable()) {
            updatePreference.setTitle(R.string.update_available);
            UpdateManager.Status lastKnownStatus = updateManager.getLastKnownStatus();
            assert lastKnownStatus != null;
            updatePreference.setSelectable(true);
            updatePreference.setEnabled(true);
            updatePreference.setOnPreferenceClickListener(preference -> {
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(lastKnownStatus.getDownload().toString()));
                DownloadManager downloadManager =
                        (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                long downloadId = downloadManager.enqueue(request);
                getActivity().registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) == downloadId) {
                            DownloadManager.Query q = new DownloadManager.Query();
                            q.setFilterById(downloadId);
                            Cursor c = downloadManager.query(q);
                            if (c.moveToFirst()) {
                                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    String local = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                                    Intent open = new Intent(Intent.ACTION_VIEW);
                                    open.setDataAndType(Uri.fromFile(new File(local)),
                                                        "application/vnd.android.package-archive");
                                    getActivity().startActivity(open);
                                }
                            }

                        }
                    }
                }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                return true;
            });
        } else {
            updatePreference.setTitle(R.string.no_update_available);
            updatePreference.setSummary(getResources().getString(
                    R.string.no_update_available_summary, updateManager.getAppBuild()));
            updatePreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(
                        "https://github.com/yawkat/fiction-android/commits/" + updateManager.getAppBuild()));
                getActivity().startActivity(intent);
                return true;
            });
        }

        Preference rebuildIndex = getPreferenceManager().findPreference("rebuild_index");
        rebuildIndex.setOnPreferenceClickListener(preference -> {
            toasts.toast("Rebuilding index");
            taskManager.execute(index::buildIndex);
            return true;
        });
    }
}
