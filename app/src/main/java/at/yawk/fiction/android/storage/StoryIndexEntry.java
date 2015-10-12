package at.yawk.fiction.android.storage;

import at.yawk.fiction.android.ProgressStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import org.joda.time.Instant;

/**
 * @author yawkat
 */
@Data
@DatabaseTable(tableName = "storyIndex")
public class StoryIndexEntry {
    @DatabaseField(id = true)
    private String storyId;
    @DatabaseField(canBeNull = true)
    private String providerId;
    @DatabaseField(canBeNull = false)
    private int totalChapterCount;
    @DatabaseField(canBeNull = false)
    private int readChapterCount;
    @DatabaseField(canBeNull = false)
    private int downloadedChapterCount;
    @DatabaseField(canBeNull = true)
    private String title;
    @DatabaseField(canBeNull = true, persisterClass = InstantPersister.class)
    private Instant lastActionTime;

    @JsonIgnore
    public ProgressStatus getReadProgressType() {
        return ProgressStatus.of(getReadChapterCount(), getTotalChapterCount());
    }

    @JsonIgnore
    public ProgressStatus getDownloadProgressType() {
        return ProgressStatus.of(getDownloadedChapterCount(), getTotalChapterCount());
    }
}
