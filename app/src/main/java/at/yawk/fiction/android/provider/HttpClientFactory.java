package at.yawk.fiction.android.provider;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.apache.OkApacheClient;
import java.net.CookieManager;
import java.net.CookiePolicy;
import javax.inject.Inject;
import javax.inject.Singleton;
import lib.org.apache.http.client.HttpClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class HttpClientFactory {
    private final ConnectionPool connectionPool = ConnectionPool.getDefault();

    @Inject
    HttpClientFactory() {}

    public HttpClient createHttpClient(OkHttpClient client) {
        client.setConnectionPool(connectionPool);
        if (client.getCookieHandler() == null) {
            client.setCookieHandler(new CookieManager());
        }
        ((CookieManager) client.getCookieHandler()).setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        // set user agent
        client.networkInterceptors().add(chain -> chain.proceed(
                chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")
                        .build()
        ));
        return new OkApacheClient(client);
    }

    public HttpClient createHttpClient() {
        return createHttpClient(new OkHttpClient());
    }
}
