package at.yawk.fiction.android.provider.fim;

import at.yawk.fiction.Story;
import at.yawk.fiction.android.inject.BaseModule;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.PageParserProvider;
import at.yawk.fiction.impl.fimfiction.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author yawkat
 */
@Singleton
public class FimAndroidFictionProvider extends AndroidFictionProvider {
    private FimFictionProvider fictionProvider;

    @Inject PageParserProvider pageParserProvider;
    @Inject ObjectMapper objectMapper;

    public FimAndroidFictionProvider() {
        super("fim", "FimFiction.net",
              FimStory.class, FimChapter.class, FimAuthor.class, FimSearchQuery.class);
    }

    @Override
    public FimFictionProvider getFictionProvider() {
        if (fictionProvider == null) {
            fictionProvider = new FimFictionProvider(pageParserProvider, createHttpClient(), objectMapper);
        }
        return fictionProvider;
    }

    @Override
    public QueryEditorFragment<?> createQueryEditorFragment() {
        return new FimQueryEditorFragment();
    }

    @Override
    public String getStoryId(Story story, String separator) {
        return String.valueOf(((FimStory) story).getId());
    }

    @Override
    public List<String> getTags(Story story) {
        return Collections.emptyList(); // todo
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = BaseModule.class, injects = FimAndroidFictionProvider.class)
    static class M {}
}
