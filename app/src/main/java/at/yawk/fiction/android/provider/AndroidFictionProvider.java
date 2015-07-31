package at.yawk.fiction.android.provider;

import at.yawk.fiction.*;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lib.org.apache.http.client.HttpClient;
import lombok.Getter;

/**
 * @author yawkat
 */
@Getter
public abstract class AndroidFictionProvider {
    private final String id;
    private final String name;
    private final Set<Class<?>> providingClasses;

    HttpClient httpClient;

    public AndroidFictionProvider(String id, String name, Class<?>... providingClasses) {
        this.id = id;
        this.name = name;
        this.providingClasses = ImmutableSet.copyOf(providingClasses);
    }

    @Inject
    public void initHttpClient(HttpClientFactory factory) {
        httpClient = factory.createHttpClient();
    }

    @JsonIgnore
    protected HttpClient getHttpClient() {
        return httpClient;
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

    @JsonIgnore
    public abstract FictionProvider getFictionProvider();

    public abstract QueryEditorFragment<?> createQueryEditorFragment();

    public abstract String getStoryId(Story story, String separator);

    public abstract List<String> getTags(Story story);
}
