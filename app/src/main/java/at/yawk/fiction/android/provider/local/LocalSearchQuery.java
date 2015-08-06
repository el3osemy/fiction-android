package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.storage.StoryWrapper;
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
        int chapterCount = wrapper.getStory().getChapters().size();
        int downloadedCount = wrapper.getDownloadedChapterCount();
        int readCount = wrapper.getReadChapterCount();

        if (readCount <= 0) {
            if (!readNone) { return false; }
        } else if (readCount >= chapterCount) {
            if (!readAll) { return false; }
        } else {
            if (!readSome) { return false; }
        }

        if (downloadedCount <= 0) {
            if (!downloadedNone) { return false; }
        } else if (downloadedCount >= chapterCount) {
            if (!downloadedAll) { return false; }
        } else {
            if (!downloadedSome) { return false; }
        }

        return true;
    }

}
