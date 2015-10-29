package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.storage.QueryWrapper;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class QueryPageDownloadTask implements DownloadTask {
    private final QueryWrapper wrapper;
    private final int page;

    @Override
    public String getName() {
        return "'" + wrapper.getName() + "' page " + page;
    }
}
