package at.yawk.fiction.android;

import android.os.Environment;
import at.yawk.fiction.Chapter;
import at.yawk.fiction.NotFoundException;
import at.yawk.fiction.android.provider.ProviderManager;
import at.yawk.fiction.android.provider.ffn.FfnAndroidFictionProvider;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import at.yawk.fiction.impl.fanfiction.FfnStory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@SuppressWarnings("unused")
@Slf4j
@Singleton
public class Importer implements Runnable {
    @Inject ObjectMapper objectMapper;
    @Inject ProviderManager providerManager;
    @Inject StoryManager storyManager;
    @Inject FfnAndroidFictionProvider provider;

    @Override
    public void run() {
        try {
            File[] files = new File(Environment.getExternalStorageDirectory(), "Fanfiction/db/story")
                    .listFiles((dir, filename) -> {
                        return filename.matches("\\d+");
                    });
            log.info("{} files found.", files.length);
            outer:
            for (int j = 0; j < files.length; j++) {
                File file = files[j];
                log.info("Visiting {}/{}: {}", j, files.length, file.getName());
                JsonNode tree = objectMapper.readTree(file);
                JsonNode chapters = tree.get("chapters");
                for (int i = 0; i < chapters.size(); i++) {
                    JsonNode chapterNode = chapters.get(i);
                    if (Objects.equal(chapterNode.get("readHash"), chapterNode.get("textHash"))) {
                        FfnStory keyStory = new FfnStory();
                        keyStory.setId(tree.get("story").get("id").asInt());
                        StoryWrapper story = storyManager.getStory(keyStory);
                        if (story.getStory() == null) {
                            log.info("Fetching story {}", keyStory.getId());
                            try {
                                provider.fetchStory(keyStory);
                            } catch (NotFoundException e) {
                                log.warn("Failed to fetch story (not found)");
                                continue outer;
                            }
                            story.updateStory(keyStory);
                        }

                        if (story.getStory().getChapters().size() <= i) {
                            log.warn("Failed to mark chapter {}+ / {} (disappeared?)", i, chapters.size());
                            continue outer;
                        }

                        Chapter chapter = story.getStory().getChapters().get(i);
                        if (!story.isChapterDownloaded(i)) {
                            log.info("Fetching chapter {}/{}", keyStory.getId(), i);
                            provider.fetchChapter(keyStory, chapter);
                            story.updateStory(keyStory);
                        }

                        story.setChapterRead(i, true);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
        log.info("Done!");
    }
}
