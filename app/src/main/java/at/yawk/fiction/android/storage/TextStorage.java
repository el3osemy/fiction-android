package at.yawk.fiction.android.storage;

import android.util.Base64;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.HtmlText;
import at.yawk.fiction.RawText;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class TextStorage {
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final int SIZE_LIMIT = 256;

    private final ObjectStorageManager objectStorage;

    @Inject
    TextStorage(ObjectStorageManager objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Nullable
    public FormattedText getText(String hash) {
        try {
            return objectStorage.load(FormattedText.class, getStorageId(hash));
        } catch (NotFoundException | UnreadableException e) {
            return null;
        }
    }

    public String externalizeText(FormattedText text) {
        String hash;
        if (text instanceof ExternalizedText) {
            hash = Base64.encodeToString(((ExternalizedText) text).hash,
                                         Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } else {
            ExternalizationStrategy<? super FormattedText> strategy = getStrategy(text);
            hash = hashToString(strategy.hash(text));
            String storageId = getStorageId(hash);
            if (!objectStorage.exists(storageId)) {
                objectStorage.save(text, storageId);
                log.debug("Externalized {} characters to {}", strategy.length(text), hash);
            }
        }
        return hash;
    }

    private static String hashToString(byte[] hash) {
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(HEX[(b >>> 4) & 0xf]);
            builder.append(HEX[b & 0xf]);
        }
        return builder.toString();
    }

    private static String getStorageId(String hash) {
        return "text/" + hash;
    }

    @SuppressWarnings("unchecked")
    private <T extends FormattedText> ExternalizationStrategy<? super T> getStrategy(T text) {
        if (text instanceof HtmlText) {
            return (ExternalizationStrategy<? super T>) HTML_TEXT_EXTERNALIZATION_STRATEGY;
        } else if (text instanceof RawText) {
            return (ExternalizationStrategy<? super T>) RAW_TEXT_EXTERNALIZATION_STRATEGY;
        } else if (text instanceof ExternalizedText) {
            return (ExternalizationStrategy<? super T>) EXTERNALIZED_TEXT_EXTERNALIZATION_STRATEGY;
        } else {
            throw new UnsupportedOperationException("Unsupported text type " + text.getClass().getName());
        }
    }

    private interface ExternalizationStrategy<T extends FormattedText> {
        byte[] hash(T text);

        boolean isExternalizable(T text);

        int length(T text);
    }

    private static final ExternalizationStrategy<HtmlText> HTML_TEXT_EXTERNALIZATION_STRATEGY =
            new ExternalizationStrategy<HtmlText>() {
                @Override
                public byte[] hash(HtmlText text) {
                    return Hashing.sha256().newHasher()
                            .putString("html", Charsets.UTF_8)
                            .putString(text.getHtml(), Charsets.UTF_8)
                            .hash().asBytes();
                }

                @Override
                public boolean isExternalizable(HtmlText text) {
                    return text.getHtml().length() > SIZE_LIMIT;
                }

                @Override
                public int length(HtmlText text) {
                    return text.getHtml().length();
                }
            };

    private static final ExternalizationStrategy<RawText> RAW_TEXT_EXTERNALIZATION_STRATEGY =
            new ExternalizationStrategy<RawText>() {
                @Override
                public byte[] hash(RawText text) {
                    return Hashing.sha256().newHasher()
                            .putString("raw", Charsets.UTF_8)
                            .putString(text.getText(), Charsets.UTF_8)
                            .hash().asBytes();
                }

                @Override
                public boolean isExternalizable(RawText text) {
                    return text.getText().length() > SIZE_LIMIT;
                }

                @Override
                public int length(RawText text) {
                    return text.getText().length();
                }
            };

    private static final ExternalizationStrategy<ExternalizedText> EXTERNALIZED_TEXT_EXTERNALIZATION_STRATEGY =
            new ExternalizationStrategy<ExternalizedText>() {
                @Override
                public byte[] hash(ExternalizedText text) {
                    return text.hash;
                }

                @Override
                public boolean isExternalizable(ExternalizedText text) {
                    return false;
                }

                @Override
                public int length(ExternalizedText text) {
                    throw new UnsupportedOperationException();
                }
            };

    public static class ExternalizedText implements FormattedText {
        @JsonProperty byte[] hash;

        ExternalizedText(byte[] hash) {
            this.hash = hash;
        }

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
