package at.yawk.fiction.android.ui;

import at.yawk.fiction.android.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
@Getter
public enum Theme {
    DARK(R.style.ThemeBaseDark, R.string.dark),
    DARK_AMOLED(R.style.ThemeAmoled, R.string.dark_amoled),;

    private final int theme;
    private final int name;
}
