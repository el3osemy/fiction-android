package at.yawk.fiction.android.provider.fim;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import at.yawk.fiction.Chapter;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.inject.BaseModule;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.PageParserProvider;
import at.yawk.fiction.impl.fimfiction.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import dagger.Module;
import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lib.org.apache.http.client.HttpClient;
import lib.org.apache.http.impl.client.BasicCookieStore;
import lib.org.apache.http.impl.cookie.BasicClientCookie;
import lombok.SneakyThrows;

/**
 * @author yawkat
 */
@Singleton
public class FimAndroidFictionProvider extends AndroidFictionProvider {
    private FimFictionProvider fictionProvider;

    @Inject PageParserProvider pageParserProvider;
    @Inject ObjectMapper objectMapper;
    @Inject SharedPreferences sharedPreferences;

    @Nullable
    private FimAuthentication lastAuthentication = null;
    private boolean useProvidedReadStatus;

    public FimAndroidFictionProvider() {
        super("fim", "FimFiction.net",
              FimStory.class, FimChapter.class, FimAuthor.class, FimSearchQuery.class);
    }

    @Override
    @SneakyThrows
    public PreferenceScreen inflatePreference(Context context, PreferenceManager manager) {
        Method inflateFromResource = PreferenceManager.class.getMethod(
                "inflateFromResource",
                Context.class, int.class, PreferenceScreen.class);
        return (PreferenceScreen) inflateFromResource.invoke(manager, context, R.xml.settings_fim, null);
    }

    @Override
    public boolean useProvidedReadStatus() {
        return useProvidedReadStatus;
    }

    @Override
    public void setRead(Story story, Chapter chapter, boolean read) throws Exception {
        while (chapter.getRead() == null ||
               chapter.getRead() != read) {
            getFictionProvider().toggleRead(chapter);
        }
    }

    @Override
    public FimFictionProvider getFictionProvider() {
        if (fictionProvider == null) {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie("view_mature", "true");
            cookie.setDomain("www.fimfiction.net");
            cookieStore.addCookie(cookie);

            HttpClient httpClient = createHttpClientBuilder()
                    .setDefaultCookieStore(cookieStore)
                    .build();
            fictionProvider = new FimFictionProvider(pageParserProvider, httpClient, objectMapper);
            sharedPreferences.registerOnSharedPreferenceChangeListener((sp, key) -> updateLogin());
            updateLogin();
        }
        return fictionProvider;
    }

    private void updateLogin() {
        String username = sharedPreferences.getString("fim.username", "");
        String password = sharedPreferences.getString("fim.password", "");
        FimAuthentication authentication;

        if (password.isEmpty() || username.isEmpty()) {
            authentication = null;
        } else {
            authentication = new FimAuthentication(username, password);
        }

        if (!Objects.equal(authentication, lastAuthentication)) {
            fictionProvider.setDefaultAuthentication(authentication);
            lastAuthentication = authentication;
        }

        useProvidedReadStatus = sharedPreferences.getBoolean("fim.useProvidedReadStatus", true);
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
        return Lists.transform(((FimStory) story).getTags(), FimTag::getName);
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = BaseModule.class, injects = FimAndroidFictionProvider.class)
    static class M {}
}
