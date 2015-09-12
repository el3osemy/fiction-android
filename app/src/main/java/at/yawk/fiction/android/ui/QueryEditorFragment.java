package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.context.WrapperParcelable;
import at.yawk.fiction.android.inject.ExternalInjectable;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author yawkat
 */
public abstract class QueryEditorFragment<S extends SearchQuery> extends ContentViewFragment
        implements ExternalInjectable {
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bind();
    }

    protected abstract void bind();

    protected void bindString(EditText editor, Getter<S, String> getter, Setter<S, String> setter) {
        editor.setText(getter.get(getQuery()));
        saveTasks.add(() -> setter.set(getQuery(), editor.getText().toString()));
    }

    protected void bindInteger(EditText editor, Getter<S, Integer> getter, Setter<S, Integer> setter) {
        Integer value = getter.get(getQuery());
        editor.setText(value == null ? null : String.valueOf(value));
        saveTasks.add(() -> {
            String text = editor.getText().toString();
            setter.set(getQuery(), text.isEmpty() ? null : Integer.valueOf(text));
        });
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
