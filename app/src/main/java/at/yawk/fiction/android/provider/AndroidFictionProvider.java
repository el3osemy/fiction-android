package at.yawk.fiction.android.provider;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import at.yawk.fiction.*;
import at.yawk.fiction.android.inject.ExternalInjectable;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.android.ui.AsyncAction;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lib.org.apache.http.client.HttpClient;
import lombok.Getter;

/**
 * @author yawkat
 */
@Getter
public abstract class AndroidFictionProvider implements ExternalInjectable {
    private final String id;
    private final String name;
    private final Set<Class<?>> providingClasses;

    @Inject StoryManager storyManager;
    @Inject @Getter HttpClientFactory httpClientFactory;

    HttpClient httpClient;

    public AndroidFictionProvider(String id, String name, Class<?>... providingClasses) {
        this.id = id;
        this.name = name;
        this.providingClasses = ImmutableSet.copyOf(providingClasses);
    }

    /**
     * Create a http client from our http client factory.
     */
    protected HttpClient createHttpClient() {
        return httpClientFactory.createHttpClient();
    }

    @JsonIgnore
    protected abstract FictionProvider getFictionProvider();

    /**
     * @see FictionProvider#fetchStory(Story)
     */
    public void fetchStory(Story story) throws Exception {
        getFictionProvider().fetchStory(story);
    }

    /**
     * @see FictionProvider#fetchChapter(Story, Chapter)
     */
    public void fetchChapter(Story story, Chapter chapter) throws Exception {
        getFictionProvider().fetchChapter(story, chapter);
    }

    /**
     * @see FictionProvider#createQuery()
     */
    public SearchQuery createQuery() {
        return getFictionProvider().createQuery();
    }

    /**
     * Search a given query, returning a list of story wrappers.
     */
    public Pageable<StoryWrapper> searchWrappers(SearchQuery query) {
        Pageable<? extends Story> pageable = getFictionProvider().search(query);
        return i -> {
            Pageable.Page<? extends Story> storyPage = pageable.getPage(i);
            Pageable.Page<StoryWrapper> wrapperPage = new Pageable.Page<>();
            wrapperPage.setEntries(Lists.transform(storyPage.getEntries(), storyManager::mergeStory));
            wrapperPage.setLast(storyPage.isLast());
            wrapperPage.setPageCount(storyPage.getPageCount());
            return wrapperPage;
        };
    }

    /**
     * @return {@code true} if the given query can be cached, {@code false} otherwise.
     */
    public boolean isQueryOfflineCacheable(SearchQuery query) {
        return true;
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

    public abstract QueryEditorFragment<?> createQueryEditorFragment();

    /**
     * Get an id of this story unique to this fiction provider (but not necessarily unique to other providers).
     */
    public abstract String getStoryId(Story story, String separator);

    /**
     * Get the tags of the given story.
     */
    public abstract List<String> getTags(Story story);

    /**
     * If the given uri is managed by this provider, return whatever story info we can deduce from it, otherwise return
     * {@code null}.
     */
    @Nullable
    public Story getStory(Uri uri) {
        return null;
    }

    /**
     * Get a list of additional actions for this story.
     */
    public List<AsyncAction> getAdditionalActions(Story story) {
        return Collections.emptyList();
    }
}
