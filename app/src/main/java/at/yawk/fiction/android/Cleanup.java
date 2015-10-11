package at.yawk.fiction.android;

import at.yawk.fiction.android.storage.FileSystemStorage;
import at.yawk.fiction.android.storage.StoryManager;
import at.yawk.fiction.android.storage.StoryWrapper;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class Cleanup implements Runnable {
    @Inject StoryManager storyManager;
    @Inject FileSystemStorage fileSystemStorage;

    @Override
    public void run() {
        log.info("Collecting text hashes...");
        Set<String> usedTextKeys = new HashSet<>();
        for (StoryWrapper story : storyManager.listStories()) {
            int chapterCount = story.getStory().getChapters().size();
            for (int i = 0; i < chapterCount; i++) {
                String hash = story.getSavedTextHash(i);
                if (hash != null) {
                    usedTextKeys.add(hash);
                }
            }
        }

        log.info("Found {} used text hashes, looking for orphans...", usedTextKeys.size());
        Set<String> deletableKeys = new HashSet<>();
        for (String key : fileSystemStorage.list("text")) {
            if (!usedTextKeys.contains(key.substring(key.indexOf('/') + 1))) {
                deletableKeys.add(key);
            }
        }

        log.info("Found {} deletable orphans, deleting...", deletableKeys.size());
        for (String key : deletableKeys) {
            fileSystemStorage.delete(key);
        }
        log.info("Done!");
    }
}
