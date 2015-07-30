package at.yawk.fiction.android.provider;

import at.yawk.fiction.*;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lib.org.apache.http.client.HttpClient;

/**
 * @author yawkat
 */
public abstract class AndroidFictionProvider {
    private transient HttpClient httpClient;

    public abstract String getName();

    public abstract String getId();

    @JsonIgnore
    protected HttpClient getHttpClient() {
        return httpClient;
    }

    @JsonIgnore
    public abstract FictionProvider getFictionProvider();

    public void init(ProviderContext context) {
        httpClient = context.createClient();
    }

    public void fetchStory(Story story) throws Exception {
        getFictionProvider().fetchStory(story);
    }

    public Pageable<? extends Story> search(SearchQuery searchQuery) {
        return getFictionProvider().search(searchQuery);
    }

    public void fetchChapter(Story story, Chapter chapter) throws Exception {
        getFictionProvider().fetchChapter(story, chapter);
    }

    public SearchQuery createQuery() {
        return getFictionProvider().createQuery();
    }

    public abstract QueryEditorFragment<?> createQueryEditorFragment();

    public abstract String getStoryId(Story story, String separator);

    public abstract List<String> getTags(Story story);
}
