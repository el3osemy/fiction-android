package at.yawk.fiction.android.storage;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import at.yawk.fiction.Chapter;
import at.yawk.fiction.FormattedText;
import at.yawk.fiction.HtmlText;
import at.yawk.fiction.RawText;
import at.yawk.fiction.Story;
import at.yawk.fiction.android.provider.AndroidFictionProvider;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
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
@Singleton
public class EpubBuilder {
    @Inject RootFile root;

    public void openEpub(Activity activity, StoryWrapper story) throws IOException {
        File file = buildEpub(story);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/epub+zip");
        activity.startActivity(intent);
    }

    public void buildEpub(StoryWrapper wrapper, OutputStream stream) throws IOException {
        Story story = wrapper.getStory();

        Book book = new Book();
        Metadata metadata = book.getMetadata();
        metadata.setTitles(Collections.singletonList(story.getTitle()));
        if (story.getAuthor() != null) {
            metadata.setAuthors(Collections.singletonList(new Author(story.getAuthor().getName())));
        }

        List<? extends Chapter> chapters = story.getChapters();
        if (chapters != null) {
            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                String name = chapter.getName();
                if (name == null) {
                    name = "Chapter " + (i + 1);
                }

                FormattedText text = wrapper.loadChapterText(i);
                Resource resource;
                if (text instanceof HtmlText) {
                    resource = createHtmlResource(Jsoup.clean(((HtmlText) text).getHtml(),
                                                              Whitelist.basicWithImages()));
                } else if (text instanceof RawText) {
                    resource = createHtmlResource(StringEscapeUtils.escapeHtml4(((RawText) text).getText()));
                } else {
                    assert text == null : "Unsupported " + text.getClass().getName();
                    resource = createHtmlResource("Not Downloaded");
                }
                book.addSection(name, resource);
            }
        }

        EpubWriter writer = new EpubWriter();
        writer.write(book, stream);
    }

    @SuppressLint("SetWorldReadable")
    public File buildEpub(StoryWrapper wrapper) throws IOException {
        Story story = wrapper.getStory();
        AndroidFictionProvider provider = wrapper.getProvider();
        String id = provider.getStoryId(story, "/");

        File file = new File(root.getRoot(), "epub/" + provider.getId() + "/" + id + ".epub");
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        FileOutputStream stream = new FileOutputStream(file);
        try {
            buildEpub(wrapper, stream);
        } finally {
            Closeables.close(stream, true);
        }

        //noinspection ResultOfMethodCallIgnored
        file.setReadable(true, false);

        return file;
    }

    private static Resource createHtmlResource(String htmlBody) {
        return new Resource(("<html><body>" + htmlBody + "</body></html>").getBytes(Charsets.UTF_8),
                            MediatypeService.XHTML);
    }
}
