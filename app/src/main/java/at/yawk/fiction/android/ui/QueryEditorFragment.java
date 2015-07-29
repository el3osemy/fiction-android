package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yawkat
 */
public abstract class QueryEditorFragment<S extends SearchQuery> extends Fragment implements ContextProvider {
    private S query;
    private FictionContext context;

    private List<Runnable> saveTasks = new ArrayList<>();

    public S getQuery() {
        return query;
    }

    public void setQuery(FictionContext context, S query) {
        this.query = query;
        Bundle bundle = new Bundle();
        bundle.putParcelable("query", context.objectToParcelable(query));
        setArguments(bundle);
    }

    @Override
    public FictionContext getContext() {
        if (context == null) {
            context = FictionContext.get(getActivity());
        }
        return context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        query = getContext().parcelableToObject(getArguments().getParcelable("query"));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return createView(inflater, container);
    }

    protected abstract View createView(LayoutInflater inflater, ViewGroup container);

    protected void bindString(EditText editor, Getter<S, String> getter, Setter<S, String> setter) {
        editor.setText(getter.get(getQuery()));
        saveTasks.add(() -> setter.set(getQuery(), editor.getText().toString()));
    }

    protected <C> void bindChoice(Spinner spinner,
                                  Getter<S, C> getter,
                                  Setter<S, C> setter,
                                  Function<C, String> toString,
                                  C[] choices) {
        bindChoice(spinner, getter, setter, toString, Arrays.asList(choices));
    }

    @SuppressWarnings("unchecked")
    protected <C> void bindChoice(Spinner spinner,
                                  Getter<S, C> getter,
                                  Setter<S, C> setter,
                                  Function<C, String> toString,
                                  List<C> choices) {
        spinner.setAdapter(new StringArrayAdapter(getActivity(), choices, toString));
        int position = choices.indexOf(getter.get(getQuery()));
        if (position != -1) { spinner.setSelection(position); }
        saveTasks.add(() -> setter.set(getQuery(), (C) spinner.getSelectedItem()));
    }

    public void save() {
        for (Runnable saveTask : saveTasks) {
            saveTask.run();
        }
    }

    public boolean isSavable() {
        return true;
    }

    public void updateSavable() {
        ((QueryWrapperActivity) getActivity()).updateSavable();
    }

    public interface Getter<O, T> {
        T get(O obj);
    }

    public interface Setter<O, T> {
        void set(O obj, T value);
    }

    /**
     * Return a list containing null and the given item array.
     */
    protected static <T> List<T> andNull(T[] items) {
        List<T> list = new ArrayList<>(items.length + 1);
        list.add(null);
        list.addAll(Arrays.asList(items));
        return list;
    }
}
