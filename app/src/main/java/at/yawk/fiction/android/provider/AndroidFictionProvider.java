package at.yawk.fiction.android.provider;

import at.yawk.fiction.*;
import at.yawk.fiction.android.provider.ffn.FfnAndroidFictionProvider;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lib.org.apache.http.client.HttpClient;

/**
 * @author yawkat
 */
@JsonSubTypes({ @JsonSubTypes.Type(FfnAndroidFictionProvider.class) })
public abstract class AndroidFictionProvider {
    private transient HttpClient httpClient;
    private transient FictionProvider fictionProvider;

    public abstract String getName();

    @JsonIgnore
    protected HttpClient getHttpClient() {
        return httpClient;
    }

    @JsonIgnore
    protected void setFictionProvider(FictionProvider fictionProvider) {
        this.fictionProvider = fictionProvider;
    }

    @JsonIgnore
    protected FictionProvider getFictionProvider() {
        return fictionProvider;
    }

    public void init(ProviderContext context) {
        httpClient = context.createClient();
    }

    public void fetchStory(Story story) {
        getFictionProvider().fetchStory(story);
    }

    public Pageable<? extends Story> search(SearchQuery searchQuery) {
        return getFictionProvider().search(searchQuery);
    }

    public void fetchChapter(Story story, Chapter chapter) {
        getFictionProvider().fetchChapter(story, chapter);
    }

    public SearchQuery createQuery() {
        return getFictionProvider().createQuery();
    }

    public abstract QueryEditorFragment<?> createQueryEditorFragment();
}
