package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import at.yawk.fiction.android.R;

/**
 * @author yawkat
 */
public class MainPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main);
    }
}
