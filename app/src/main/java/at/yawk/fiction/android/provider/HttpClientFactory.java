package at.yawk.fiction.android.provider;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;
import lib.org.apache.http.client.HttpClient;
import lib.org.apache.http.impl.client.HttpClientBuilder;
import lib.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import lib.org.apache.http.message.BasicHeader;

/**
 * @author yawkat
 */
@Singleton
public class HttpClientFactory {
    private final PoolingHttpClientConnectionManager connectionManager;

    @Inject
    HttpClientFactory() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(2);
    }

    public HttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultHeaders(Collections.singleton(new BasicHeader(
                        "User-Agent", "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0")))
                .build();
    }
}
