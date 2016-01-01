package at.yawk.fiction.android.provider.fim;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import at.yawk.fiction.Chapter;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.R;
import at.yawk.fiction.android.context.TaskManager;
import at.yawk.fiction.android.inject.BaseModule;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.Provider;
import at.yawk.fiction.android.ui.AsyncAction;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.PageParserProvider;
import at.yawk.fiction.impl.fimfiction.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lib.org.apache.http.client.HttpClient;
import lombok.SneakyThrows;

/**
 * @author yawkat
 */
@Singleton
@Provider(priority = 2000)
public class FimAndroidFictionProvider extends AndroidFictionProvider {
    private FimFictionProvider fictionProvider;

    @Inject PageParserProvider pageParserProvider;
    @Inject ObjectMapper objectMapper;
    @Inject SharedPreferences sharedPreferences;
    @Inject TaskManager taskManager;

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
        getFictionProvider(); // make sure we're initialized
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
    @SneakyThrows(IOException.class)
    public FimFictionProvider getFictionProvider() {
        if (fictionProvider == null) {
            CookieHandler cookieMgr = new CookieManager();
            cookieMgr.put(URI.create("https://www.fimfiction.net/"),
                          ImmutableMap.of("Set-Cookie", Collections.singletonList("view_mature=true")));

            OkHttpClient okClient = new OkHttpClient();
            okClient.setCookieHandler(cookieMgr);

            HttpClient httpClient = getHttpClientFactory().createHttpClient(okClient);
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
        List<String> tags = new ArrayList<>();
        FimStatus status = ((FimStory) story).getStatus();
        if (status != null) {
            tags.add(status.name());
        }
        for (FimTag tag : ((FimStory) story).getTags()) {
            tags.add(tag.getName());
        }
        return tags;
    }

    @Nullable
    @Override
    public Story getStory(Uri uri) {
        if (uri.getHost().matches("(www\\.)?fimfiction\\.net")) {
            Pattern pathPattern = Pattern.compile("/story/(\\d+)(/.*)?");
            Matcher pathMatcher = pathPattern.matcher(uri.getPath());
            if (pathMatcher.matches()) {
                FimStory fimStory = new FimStory();
                fimStory.setId(Integer.parseInt(pathMatcher.group(1)));
                return fimStory;
            }
        }
        return super.getStory(uri);
    }

    @Override
    public List<AsyncAction> getAdditionalActions(Story story) {
        return Collections.singletonList(new AsyncAction(
                R.string.manage_shelves,
                ctx -> {
                    Map<FimShelf, Boolean> shelves;
                    try {
                        shelves = fictionProvider.fetchStoryShelves(story);
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }

                    FimShelf[] shelfArray = new FimShelf[shelves.size()];
                    CharSequence[] nameArray = new CharSequence[shelves.size()];
                    boolean[] statusArray = new boolean[shelves.size()];
                    int i = 0;
                    for (Map.Entry<FimShelf, Boolean> entry : shelves.entrySet()) {
                        shelfArray[i] = entry.getKey();
                        nameArray[i] = entry.getKey().getName();
                        statusArray[i] = entry.getValue();
                        i++;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx.getContext());
                    builder.setMultiChoiceItems(
                            nameArray,
                            statusArray,
                            (dialog, which, isChecked) -> {
                                taskManager.execute(ctx.getTaskContext(), () -> {
                                    try {
                                        fictionProvider.setStoryShelf(story, shelfArray[which], isChecked);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                            }
                    );
                    ctx.getUiRunner().runOnUiThread(builder::show);
                }
        ));
    }

    @Override
    public Object createModule() {
        return new M();
    }

    @Module(addsTo = BaseModule.class, injects = FimAndroidFictionProvider.class)
    static class M {}
}
