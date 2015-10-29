package at.yawk.fiction.android.download.task;

import at.yawk.fiction.android.storage.QueryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class QueryDownloadTask implements SplittableDownloadTask {
    private final QueryWrapper wrapper;
    // todo: do this iteratively, instead of requiring the page count here
    private final int pages;

    @Override
    public String getName() {
        return "'" + wrapper.getName() + "'";
    }

    @Override
    public List<DownloadTask> getTasks() {
        ArrayList<DownloadTask> tasks = new ArrayList<>(pages);
        for (int i = 0; i < pages; i++) {
            tasks.add(new QueryPageDownloadTask(wrapper, i));
        }
        return tasks;
    }
}
