package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.SearchQuery;
import at.yawk.fiction.android.ProgressStatus;
import at.yawk.fiction.android.storage.StoryIndexEntry;
import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.HashSet;
import java.util.Set;
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

    Set<String> excludedProviders = new HashSet<>();

    StoryOrder order = StoryOrder.ALPHABETICAL;

    boolean acceptIndex(StoryIndexEntry indexEntry) {
        if (excludedProviders.contains(indexEntry.getProviderId())) {
            return false;
        }

        return accept(indexEntry.getReadProgressType(), indexEntry.getDownloadProgressType());
    }

    boolean accept(StoryWrapper wrapper) {
        if (excludedProviders.contains(wrapper.getProvider().getId())) {
            return false;
        }

        return accept(wrapper.getReadProgressType(), wrapper.getDownloadProgressType());

    }

    private boolean accept(ProgressStatus readStatus, ProgressStatus downloadStatus) {
        switch (readStatus) {
        case NONE:
            if (!readNone) { return false; }
            break;
        case SOME:
            if (!readSome) { return false; }
            break;
        case ALL:
            if (!readAll) { return false; }
            break;
        }

        switch (downloadStatus) {
        case NONE:
            if (!downloadedNone) { return false; }
            break;
        case SOME:
            if (!downloadedSome) { return false; }
            break;
        case ALL:
            if (!downloadedAll) { return false; }
            break;
        }
        return true;
    }
}
