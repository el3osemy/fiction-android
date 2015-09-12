package at.yawk.fiction.android.provider;

import android.content.Context;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import at.yawk.fiction.*;
import at.yawk.fiction.android.inject.ExternalInjectable;
import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lib.org.apache.http.client.HttpClient;
import lib.org.apache.http.impl.client.HttpClientBuilder;
import lombok.Getter;

/**
 * @author yawkat
 */
@Getter
public abstract class AndroidFictionProvider implements ExternalInjectable {
    private final String id;
    private final String name;
    private final Set<Class<?>> providingClasses;

    @Inject StorageManager storageManager;
    @Inject HttpClientFactory httpClientFactory;

    HttpClient httpClient;

    public AndroidFictionProvider(String id, String name, Class<?>... providingClasses) {
        this.id = id;
        this.name = name;
        this.providingClasses = ImmutableSet.copyOf(providingClasses);
    }

    protected HttpClient createHttpClient() {
        return httpClientFactory.createHttpClient();
    }

    protected HttpClientBuilder createHttpClientBuilder() {
        return httpClientFactory.createHttpClientBuilder();
    }

    public void fetchStory(Story story) throws Exception {
        getFictionProvider().fetchStory(story);
    }

    public Pageable<? extends Story> search(SearchQuery searchQuery) {
        return getFictionProvider().search(searchQuery);
    }

    public Pageable<StoryWrapper> searchWrappers(SearchQuery query) {
        Pageable<? extends Story> pageable = search(query);
        return i -> {
            Pageable.Page<? extends Story> storyPage = pageable.getPage(i);
            Pageable.Page<StoryWrapper> wrapperPage = new Pageable.Page<>();
            wrapperPage.setEntries(Lists.transform(storyPage.getEntries(), storageManager::mergeStory));
            wrapperPage.setLast(storyPage.isLast());
            wrapperPage.setPageCount(storyPage.getPageCount());
            return wrapperPage;
        };
    }

    public void fetchChapter(Story story, Chapter chapter) throws Exception {
        getFictionProvider().fetchChapter(story, chapter);
    }

    public SearchQuery createQuery() {
        return getFictionProvider().createQuery();
    }

    @Nullable
    public PreferenceScreen inflatePreference(Context context, PreferenceManager manager) {
        return null;
    }

    /**
     * Whether the read status system provided in {@link Chapter#read} should be used. If {@code false}, the local read
     * status system will be used.
     *
     * If {@code true} is returned, {@link #setRead(Story, Chapter, boolean)} must be supported.
     */
    public boolean useProvidedReadStatus() {
        return false;
    }

    /**
     * Mark the given chapter as read.
     */
    public void setRead(Story story, Chapter chapter, boolean read) throws Exception {
        throw new UnsupportedOperationException();
    }

    @JsonIgnore
    public abstract FictionProvider getFictionProvider();

    public abstract QueryEditorFragment<?> createQueryEditorFragment();

    public abstract String getStoryId(Story story, String separator);

    public abstract List<String> getTags(Story story);
}
