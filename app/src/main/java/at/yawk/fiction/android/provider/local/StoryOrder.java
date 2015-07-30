package at.yawk.fiction.android.provider.local;

import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
    };
    private final String name;
}
