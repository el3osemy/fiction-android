package at.yawk.fiction.android.provider.ffn;

import android.net.Uri;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.inject.BaseModule;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.Provider;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.PageParserProvider;
import at.yawk.fiction.impl.fanfiction.*;
import dagger.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
@Provider(priority = 1000)
public class FfnAndroidFictionProvider extends AndroidFictionProvider {
    private FfnFictionProvider fictionProvider;

    @Inject PageParserProvider pageParserProvider;

    public FfnAndroidFictionProvider() {
        super("ffn", "FanFiction.net",
              FfnSearchQuery.class, FfnStory.class, FfnChapter.class, FfnAuthor.class);
    }

    @Override
    public FfnFictionProvider getFictionProvider() {
        if (fictionProvider == null) {
            fictionProvider = new FfnFictionProvider(pageParserProvider, createHttpClient());
        }
        return fictionProvider;
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
        if (((FfnStory) story).getGenres() != null) {
            for (FfnGenre genre : ((FfnStory) story).getGenres()) {
                list.add(genre.getName());
            }
        }
        return list;
    }

    @Nullable
    @Override
    public Story getStory(Uri uri) {
        if (uri.getHost().matches("(www\\.|m\\.|)fanfiction\\.net")) {
            Pattern pathPattern = Pattern.compile("/s/(\\d+)(/.*)?");
            Matcher pathMatcher = pathPattern.matcher(uri.getPath());
            if (pathMatcher.matches()) {
                FfnStory ffnStory = new FfnStory();
                ffnStory.setId(Integer.parseInt(pathMatcher.group(1)));
                return ffnStory;
            }
        }
        return super.getStory(uri);
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = BaseModule.class, injects = FfnAndroidFictionProvider.class)
    static class M {}
}
