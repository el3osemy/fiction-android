package at.yawk.fiction.android.event;

import at.yawk.fiction.android.storage.StoryWrapper;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
public class StoryUpdateEvent {
    private final StoryWrapper story;
}
