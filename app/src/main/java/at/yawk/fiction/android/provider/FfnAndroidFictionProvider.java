package at.yawk.fiction.android.provider;

import at.yawk.fiction.impl.fanfiction.FfnFictionProvider;

/**
 * @author yawkat
 */
public class FfnAndroidFictionProvider extends AndroidFictionProvider {
    @Override
    public void init(ProviderContext context) {
        super.init(context);

        setFictionProvider(new FfnFictionProvider(context.getPageParserProvider(), getHttpClient()));
    }
}
