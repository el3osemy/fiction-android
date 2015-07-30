package at.yawk.fiction.android.provider.ffn;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderContext;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fanfiction.FfnFictionProvider;
import at.yawk.fiction.impl.fanfiction.FfnGenre;
import at.yawk.fiction.impl.fanfiction.FfnStory;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yawkat
 */
public class FfnAndroidFictionProvider extends AndroidFictionProvider {
    @Override
    public String getName() {
        return "FanFiction.net";
    }

    @Override
    public String getId() {
        return "ffn";
    }

    @Override
    public FfnFictionProvider getFictionProvider() {
        return (FfnFictionProvider) super.getFictionProvider();
    }

    @Override
    public void init(ProviderContext context) {
        super.init(context);

        setFictionProvider(new FfnFictionProvider(context.getPageParserProvider(), getHttpClient()));
    }

    @Override
    public QueryEditorFragment<?> createQueryEditorFragment() {
        return new FfnQueryEditorFragment();
    }

    @Override
    public String getStoryId(Story story, String separator) {
        return Integer.toString(((FfnStory) story).getId());
    }

    @Override
    public List<String> getTags(Story story) {
        List<String> list = new ArrayList<>();
        list.add("Favorites: " + ((FfnStory) story).getFavorites());
        list.add("Follows: " + ((FfnStory) story).getFollows());
        list.add("Words: " + ((FfnStory) story).getWords());
        for (FfnGenre genre : ((FfnStory) story).getGenres()) {
            list.add(genre.getName());
        }
        return list;
    }
}
