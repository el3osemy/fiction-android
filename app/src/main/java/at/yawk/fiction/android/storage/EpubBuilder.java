package at.yawk.fiction.android.storage;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import at.yawk.fiction.*;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import nl.siegmann.epublib.service.MediatypeService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EpubBuilder {
    private final StorageManager storageManager;
    private final File root;

    public void openEpub(Activity activity, StoryWrapper story) throws IOException {
        File file = buildEpub(story);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), MimeTypeMap.getSingleton().getMimeTypeFromExtension("epub"));
        activity.startActivity(intent);
    }

    public File buildEpub(StoryWrapper wrapper) throws IOException {
        Story story = wrapper.getStory();
        AndroidFictionProvider provider = storageManager.providerManager.getProvider(story);
        String id = provider.getStoryId(story, "/");
        File file = new File(root, "epub/" + provider.getId() + "/" + id + ".epub");
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        Book book = new Book();
        Metadata metadata = book.getMetadata();
        metadata.setTitles(Collections.singletonList(story.getTitle()));
        if (story.getAuthor() != null) {
            metadata.setAuthors(Collections.singletonList(new Author(story.getAuthor().getName())));
        }

        List<? extends Chapter> chapters = story.getChapters();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            String name = chapter.getName();
            if (name == null) {
                name = "Chapter " + (i + 1);
            }

            FormattedText text = wrapper.loadChapterText(i);
            Resource resource;
            if (text instanceof HtmlText) {
                resource = createHtmlResource(Jsoup.clean(((HtmlText) text).getHtml(), Whitelist.basicWithImages()));
            } else if (text instanceof RawText) {
                resource = createHtmlResource(StringEscapeUtils.escapeHtml4(((RawText) text).getText()));
            } else {
                assert text == null : "Unsupported " + text.getClass().getName();
                resource = createHtmlResource("Not Downloaded");
            }
            book.addSection(name, resource);
        }

        EpubWriter writer = new EpubWriter();
        FileOutputStream stream = new FileOutputStream(file);
        try {
            writer.write(book, stream);
        } finally {
            Closeables.close(stream, true);
        }

        return file;
    }

    private static Resource createHtmlResource(String htmlBody) {
        return new Resource(("<html><body>" + htmlBody + "</body></html>").getBytes(Charsets.UTF_8),
                            MediatypeService.XHTML);
    }
}
