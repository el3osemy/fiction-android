package at.yawk.fiction.android.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.google.common.base.Function;
import java.util.Arrays;
import java.util.List;

/**
 * @author yawkat
 */
public class StringArrayAdapter<T> extends ArrayAdapter<T> {
    private static final int DEFAULT_VIEW = android.R.layout.simple_list_item_1;

    private final int textViewResourceId;
    private final Function<T, String> toString;

    public StringArrayAdapter(Context context, T[] objects, Function<T, String> toString) {
        this(context, DEFAULT_VIEW, objects, toString);
    }

    public StringArrayAdapter(Context context, List<T> objects, Function<T, String> toString) {
        this(context, DEFAULT_VIEW, objects, toString);
    }

    public StringArrayAdapter(Context context, int textViewResourceId, T[] objects, Function<T, String> toString) {
        this(context, textViewResourceId, Arrays.asList(objects), toString);
    }

    public StringArrayAdapter(Context context, int textViewResourceId, List<T> objects, Function<T, String> toString) {
        super(context, textViewResourceId, objects);
        this.textViewResourceId = textViewResourceId;
        this.toString = toString;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(textViewResourceId, parent, false);
        }
        ((TextView) convertView).setText(toString.apply(getItem(position)));
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}
