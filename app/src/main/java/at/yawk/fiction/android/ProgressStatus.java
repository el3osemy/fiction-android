package at.yawk.fiction.android;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@Getter
@RequiredArgsConstructor
public enum ProgressStatus {
    NONE(R.color.chaptersReadNone),
    SOME(R.color.chaptersReadSome),
    ALL(R.color.chaptersReadAll);

    private final int colorResource;

    public static ProgressStatus of(int current, int max) {
        if (current <= 0) {
            return NONE;
        } else if (current < max) {
            return SOME;
        } else {
            return ALL;
        }
    }
}
