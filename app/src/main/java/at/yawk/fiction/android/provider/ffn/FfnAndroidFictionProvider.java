package at.yawk.fiction.android.provider.ffn;

import at.yawk.fiction.android.provider.AndroidFictionProvider;
import at.yawk.fiction.android.provider.ProviderContext;
import at.yawk.fiction.android.ui.QueryEditorFragment;
import at.yawk.fiction.impl.fanfiction.FfnFictionProvider;

/**
 * @author yawkat
 */
public class FfnAndroidFictionProvider extends AndroidFictionProvider {
    @Override
    public String getName() {
        return "FanFiction.net";
    }

    @Override
    public FfnFictionProvider getFictionProvider() {
        return (FfnFictionProvider) super.getFictionProvider();
    }

    @Override
    public void init(ProviderContext context) {
        super.init(context);

        setFictionProvider(new FfnFictionProvider(context.getPageParserProvider(), getHttpClient()));
    }

    @Override
    public QueryEditorFragment<?> createQueryEditorFragment() {
        return new FfnQueryEditorFragment();
    }
}
