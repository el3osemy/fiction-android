package at.yawk.fiction.android.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.List;

/**
 * @author yawkat
 */
public abstract class SimpleArrayAdapter<T> extends ArrayAdapter<T> {
    private final int viewResourceId;

    public SimpleArrayAdapter(Context context, int viewResourceId, List<T> objects) {
        super(context, 0, objects);
        this.viewResourceId = viewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(viewResourceId, parent, false);
        }
        decorateView(convertView, position);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    protected abstract void decorateView(View view, int position);
}
