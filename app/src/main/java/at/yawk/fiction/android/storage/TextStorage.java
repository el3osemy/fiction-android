package at.yawk.fiction.android.storage;

import android.util.Base64;
import at.yawk.fiction.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import java.util.Arrays;
import java.util.List;

/**
 * @author yawkat
 */
public class TextStorage {
    private static final int SIZE_LIMIT = 256;

    private final ObjectStorageManager objectStorage;

    TextStorage(ObjectStorageManager objectStorage) {
        this.objectStorage = objectStorage;
    }

    public FormattedText load(FormattedText text) {
        if (text instanceof ExternalizedText) {
            try {
                return objectStorage.load(FormattedText.class, getStorageId(((ExternalizedText) text).hash));
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return text;
        }
    }

    public void externalize(Story story) {
        FormattedText description = story.getDescription();
        if (description != null) {
            story.setDescription(externalize(description));
        }

        List<? extends Chapter> chapters = story.getChapters();
        if (chapters != null) {
            for (Chapter chapter : chapters) {
                externalize(chapter);
            }
        }
    }

    public void externalize(Chapter chapter) {
        FormattedText text = chapter.getText();
        if (text != null) {
            chapter.setText(externalize(text));
        }
    }

    public <T extends FormattedText> FormattedText externalize(T text) {
        ExternalizationStrategy<? super T> strategy = getStrategy(text);
        if (strategy.isExternalizable(text)) {
            byte[] hash = strategy.hash(text);
            String encoded = getStorageId(hash);
            if (!objectStorage.exists(encoded)) {
                synchronized (this) {
                    objectStorage.save(text, encoded);
                }
            }
            return new ExternalizedText(hash);
        } else {
            return text;
        }
    }

    private static String getStorageId(byte[] hash) {
        return "text/" + Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
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
