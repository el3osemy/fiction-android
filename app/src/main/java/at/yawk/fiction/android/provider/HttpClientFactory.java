package at.yawk.fiction.android.provider;

import javax.inject.Inject;
import javax.inject.Singleton;
import lib.org.apache.http.client.HttpClient;
import lib.org.apache.http.conn.HttpClientConnectionManager;
import lib.org.apache.http.impl.client.HttpClientBuilder;
import lib.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * @author yawkat
 */
@Singleton
public class HttpClientFactory {
    private final HttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

    @Inject
    HttpClientFactory() {}

    public HttpClient createHttpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .build();
    }
}
