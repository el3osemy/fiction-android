package at.yawk.fiction.android.ui;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.google.common.base.Function;
import java.util.Arrays;
import java.util.List;

/**
 * @author yawkat
 */
public class StringArrayAdapter<T> extends SimpleArrayAdapter<T> {
    private static final int DEFAULT_VIEW = android.R.layout.simple_list_item_1;

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
        this.toString = toString;
    }

    @Override
    protected void decorateView(View view, int position) {
        ((TextView) view).setText(toString.apply(getItem(position)));
    }
}
