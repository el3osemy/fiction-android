package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.Chapter;
import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author yawkat
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocalSearchQuery extends SearchQuery {
    boolean readNone = false;
    boolean readSome = true;
    boolean readAll = true;

    boolean downloadedNone = true;
    boolean downloadedSome = true;
    boolean downloadedAll = true;

    StoryOrder order = StoryOrder.ALPHABETICAL;

    boolean accept(StoryWrapper wrapper) {
        List<? extends Chapter> chapters = wrapper.getStory().getChapters();
        int downloadedCount = 0;
        for (Chapter chapter : chapters) {
            if (chapter.getText() != null) {
                downloadedCount++;
            }
        }
        int readCount = wrapper.getReadChapterCount();

        if (readCount <= 0) {
            if (!readNone) { return false; }
        } else if (readCount >= chapters.size()) {
            if (!readAll) { return false; }
        } else {
            if (!readSome) { return false; }
        }

        if (downloadedCount <= 0) {
            if (!downloadedNone) { return false; }
        } else if (downloadedCount >= chapters.size()) {
            if (!downloadedAll) { return false; }
        } else {
            if (!downloadedSome) { return false; }
        }

        return true;
    }
}
