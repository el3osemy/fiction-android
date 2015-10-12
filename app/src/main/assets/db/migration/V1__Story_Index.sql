CREATE TABLE storyIndex (
  storyId TEXT PRIMARY KEY,
  providerId TEXT,
  totalChapterCount INTEGER,
  readChapterCount INTEGER,
  downloadedChapterCount INTEGER,
  title TEXT,
  lastActionTime INTEGER
);