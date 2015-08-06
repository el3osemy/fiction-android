package at.yawk.fiction.android.download;

import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class ChapterRangeDownloadTask implements SplittableDownloadTask {
    /**
     * Story to download chapters for.
     */
    private final StoryWrapper story;
    /**
     * First chapter to download, inclusive.
     */
    private final int from;
    /**
     * Maximum chapter to download, exclusive.
     */
    private final int to;
    /**
     * If set to <code>true</code>, chapters that already have local text will be skipped.
     */
    private final boolean skipWhereTextPresent;

    @Override
    public List<DownloadTask> getTasks() {
        List<DownloadTask> tasks = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) {
            if (!skipWhereTextPresent || !story.isChapterDownloaded(i)) {
                tasks.add(new ChapterDownloadTask(story, i));
            }
        }
        return tasks;
    }

    @Override
    public String getName() {
        StringBuilder builder = new StringBuilder().append("'")
                .append(story.getStory().getTitle()).append("'");
        if (from + 1 < to) {
            builder.append(" chapters ").append(from + 1).append(" to ").append(to);
        } else {
            builder.append(" chapter ").append(from + 1);
        }
        return builder.toString();
    }
}
