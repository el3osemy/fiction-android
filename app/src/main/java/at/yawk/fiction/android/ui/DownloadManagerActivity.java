package at.yawk.fiction.android.ui;

import android.os.Bundle;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.ContentView;

/**
 * @author yawkat
 */
@ContentView(R.layout.single_frame_layout)
public class DownloadManagerActivity extends ContentViewActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, new DownloadManagerFragment())
                .commit();
    }
}

