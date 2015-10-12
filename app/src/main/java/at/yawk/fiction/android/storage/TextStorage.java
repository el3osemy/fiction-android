package at.yawk.fiction.android.storage;

import at.yawk.fiction.FormattedText;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * @author yawkat
 */
@Deprecated
public class TextStorage {
    public static class ExternalizedText implements FormattedText {
        @JsonProperty byte[] hash;

        // jackson constructor
        @SuppressWarnings("unused")
        ExternalizedText() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof ExternalizedText)) { return false; }
            ExternalizedText that = (ExternalizedText) o;
            return Arrays.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash);
        }
    }
}
