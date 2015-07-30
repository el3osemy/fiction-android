package at.yawk.fiction.android.provider;

import at.yawk.fiction.*;
import at.yawk.fiction.android.provider.ffn.FfnAndroidFictionProvider;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.List;
import lib.org.apache.http.client.HttpClient;

/**
 * @author yawkat
 */
@JsonSubTypes({ @JsonSubTypes.Type(FfnAndroidFictionProvider.class) })
public abstract class AndroidFictionProvider {
    private transient HttpClient httpClient;
    private transient FictionProvider fictionProvider;

    public abstract String getName();

    public abstract String getId();

    @JsonIgnore
    protected HttpClient getHttpClient() {
        return httpClient;
    }

    @JsonIgnore
    protected void setFictionProvider(FictionProvider fictionProvider) {
        this.fictionProvider = fictionProvider;
    }

    @JsonIgnore
    public FictionProvider getFictionProvider() {
        return fictionProvider;
    }

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
