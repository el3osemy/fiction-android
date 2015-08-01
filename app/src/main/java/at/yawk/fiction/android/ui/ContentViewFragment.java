package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContentView;

/**
 * @author yawkat
 */
public abstract class ContentViewFragment extends RoboFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getClass().getAnnotation(ContentView.class).value(), container, false);
    }
}
