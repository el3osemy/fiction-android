package at.yawk.fiction.android.storage;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yawkat
 */
@Slf4j
@Singleton
public class FileSystemStorage {
    @Inject RootFile root;
    @Inject ObjectMapper objectMapper;

    public <T> T load(Class<T> type, String key) throws NotFoundException, UnreadableException {
        File file = new File(root.getRoot(), key);
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, type);
            } catch (JsonMappingException e) {
                throw new UnreadableException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new NotFoundException();
        }
    }

    public void save(Object o, String key) {
        File file = new File(root.getRoot(), key);
        File tmp = new File(root.getRoot(), key + ".tmp");
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
        return new File(root.getRoot(), key).exists();
    }

    public void delete(String key) {
        File file = new File(root.getRoot(), key);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public Iterable<String> list(String key) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private DirectoryNode node = new DirectoryNode(null, key + '/', new File(root.getRoot(), key));
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
                            if (entries == null || index >= entries.length) {
                                node = parent;
                                return;
                            }

                            String entry = entries[index++];
                            if (entry.endsWith(".tmp")) { return; }

                            String entryPath = path + entry;
                            File file = new File(root.getRoot(), entryPath);
                            if (file.isDirectory()) {
                                node = new DirectoryNode(this, entryPath + '/', file);
                            } else {
                                next = entryPath;
                            }
                        }
                    }
                };
            }
        };
    }
}
