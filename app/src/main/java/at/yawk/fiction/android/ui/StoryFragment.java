package at.yawk.fiction.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.HtmlText;
import at.yawk.fiction.RawText;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.ContextProvider;
import at.yawk.fiction.android.context.FictionContext;
import at.yawk.fiction.android.storage.StoryWrapper;

/**
 * @author yawkat
 */
public class StoryFragment extends Fragment implements ContextProvider {
    private StoryWrapper wrapper;
    private FictionContext fictionContext;
    private View root;

    @Override
    public FictionContext getContext() {
        if (fictionContext == null) { fictionContext = FictionContext.get(getActivity()); }
        return fictionContext;
    }

    public static StoryFragment create(FictionContext context, StoryWrapper story) {
        StoryFragment fragment = new StoryFragment();
        Bundle args = new Bundle();
        args.putParcelable("story", context.objectToParcelable(story.getStory()));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wrapper = getContext().getStorageManager()
                .getStory(getContext().<Story>parcelableToObject(getArguments().getParcelable("story")));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.story, container, false);
        refresh();
        return root;
    }

    private void refresh() {
        ((TextView) root.findViewById(R.id.title)).setText(wrapper.getStory().getTitle());
        ((TextView) root.findViewById(R.id.author)).setText(wrapper.getStory().getAuthor().getName());

        TextView descriptionView = (TextView) root.findViewById(R.id.description);
        FormattedText description = wrapper.getStory().getDescription();
        if (description instanceof HtmlText) {
            descriptionView.setText(Html.fromHtml(((HtmlText) description).getHtml()));
        } else if (description instanceof RawText) {
            descriptionView.setText(((RawText) description).getText());
        } else {
            descriptionView.setText("");
        }
    }
}
