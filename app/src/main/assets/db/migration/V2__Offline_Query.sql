CREATE TABLE offlineQuery (
  queryId   TEXT,
  pageIndex INTEGER,
  itemIndex INTEGER,
  storyId   TEXT NOT NULL,
  PRIMARY KEY (queryId, pageIndex, itemIndex)
);