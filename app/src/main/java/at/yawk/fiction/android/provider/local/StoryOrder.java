package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.joda.time.Instant;

/**
 * @author yawkat
 */
@Getter
@RequiredArgsConstructor
public enum StoryOrder implements Comparator<StoryWrapper> {
    ALPHABETICAL("Alphabetical") {
        @Override
        public int compare(StoryWrapper lhs, StoryWrapper rhs) {
            return lhs.getStory().getTitle().compareToIgnoreCase(rhs.getStory().getTitle());
        }
    },
    LAST_ACTION_TIME("Last Action Time") {
        @Override
        public int compare(StoryWrapper lhs, StoryWrapper rhs) {
            Instant left = lhs.getLastActionTime();
            Instant right = rhs.getLastActionTime();
            if (left == null) {
                if (right == null) {
                    // when last action time is null, fall back to alphabetical so the list is somewhat readable.
                    return ALPHABETICAL.compare(lhs, rhs);
                } else {
                    return 1;
                }
            } else {
                return right == null ? -1 : right.compareTo(left);
            }
        }
    };
    private final String name;
}
