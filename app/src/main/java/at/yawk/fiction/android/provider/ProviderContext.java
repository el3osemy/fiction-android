package at.yawk.fiction.android.provider;

import at.yawk.fiction.android.storage.StorageManager;
import at.yawk.fiction.impl.PageParserProvider;
import lib.org.apache.http.client.HttpClient;
import lib.org.apache.http.conn.HttpClientConnectionManager;
import lib.org.apache.http.impl.client.HttpClientBuilder;
import lib.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class ProviderContext {
    @Getter
    private final PageParserProvider pageParserProvider =
            new PageParserProvider();
    private final HttpClientConnectionManager connectionManager =
            new PoolingHttpClientConnectionManager();
    @Getter
    private final StorageManager storageManager;

    public HttpClient createClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .build();
    }
}
