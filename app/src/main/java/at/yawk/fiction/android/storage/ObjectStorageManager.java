package at.yawk.fiction.android.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@RequiredArgsConstructor
class ObjectStorageManager {
    static final Joiner SLASH_JOINER = Joiner.on('/');

    private final File root;
    private final ObjectMapper objectMapper;

    public <T> T load(Class<T> type, String key) throws NotFoundException {
        File file = new File(root, key);
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NotFoundException();
        }
    }

    public void save(Object o, String key) {
        File file = new File(root, key);
        File tmp = new File(root, key + ".tmp");
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        try {
            objectMapper.writeValue(tmp, o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (file.exists() && !file.delete()) {
            throw new RuntimeException();
        }
        if (!tmp.renameTo(file)) {
            throw new RuntimeException();
        }
    }

    public boolean exists(String key) {
        return new File(root, key).exists();
    }

    public Iterable<String> list(String key) {
        return () -> new Iterator<String>() {
            private DirectoryNode node = new DirectoryNode(null, key + '/', new File(root, key));
            private String next;

            @Override
            public boolean hasNext() {
                findNext();
                return next != null;
            }

            @Override
            public String next() {
                findNext();
                if (next == null) { throw new NoSuchElementException(); }
                String e = this.next;
                next = null;
                return e;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            void findNext() {
                while (node != null && next == null) {
                    node.findNextHere();
                }
            }

            class DirectoryNode {
                final DirectoryNode parent;
                final String path;
                final String[] entries;
                int index = 0;

                DirectoryNode(DirectoryNode parent, String path, File dir) {
                    this.parent = parent;
                    this.path = path;
                    this.entries = dir.list();
                }

                void findNextHere() {
                    if (index >= entries.length) {
                        node = parent;
                        return;
                    }

                    String entry = entries[index++];
                    if (entry.endsWith(".tmp")) { return; }

                    String entryPath = path + entry;
                    File file = new File(root, entryPath);
                    if (file.isDirectory()) {
                        node = new DirectoryNode(this, entryPath + '/', file);
                    } else {
                        next = entryPath;
                    }
                }
            }
        };
    }
}
