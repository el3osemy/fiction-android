package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.Injector;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderManager;
import javax.inject.Inject;

/**
 * @author yawkat
 */
public class MainPreferenceFragment extends PreferenceFragment {
    @Inject ProviderManager providerManager;

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
    }
}
