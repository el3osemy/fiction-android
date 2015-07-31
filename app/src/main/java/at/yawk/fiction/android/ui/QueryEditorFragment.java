package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.context.WrapperParcelable;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import roboguice.fragment.RoboFragment;

/**
 * @author yawkat
 */
public abstract class QueryEditorFragment<S extends SearchQuery> extends RoboFragment {
    private S query;

    private List<Runnable> saveTasks = new ArrayList<>();

    public S getQuery() {
        return query;
    }

    public void setQuery(S query) {
        this.query = query;
        Bundle bundle = new Bundle();
        bundle.putParcelable("query", WrapperParcelable.objectToParcelable(query));
        setArguments(bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        query = WrapperParcelable.parcelableToObject(getArguments().getParcelable("query"));
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
        setChoice(spinner, toString, choices, choices.indexOf(getter.get(getQuery())));
        saveTasks.add(() -> setter.set(getQuery(), (C) spinner.getSelectedItem()));
    }

    protected <C> void setChoice(Spinner spinner,
                                 Function<C, String> toString,
                                 List<C> choices,
                                 int currentChoiceIndex) {
        spinner.setAdapter(new StringArrayAdapter<>(getActivity(), choices, toString));
        spinner.setSelection(currentChoiceIndex);
    }

    protected void clearChoice(Spinner spinner) {
        spinner.setAdapter(new ArrayAdapter<>(getActivity(), 0, Collections.emptyList()));
    }

    protected void bindBoolean(CheckBox editor, Getter<S, Boolean> getter, Setter<S, Boolean> setter) {
        editor.setChecked(getter.get(getQuery()));
        saveTasks.add(() -> setter.set(getQuery(), editor.isChecked()));
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
