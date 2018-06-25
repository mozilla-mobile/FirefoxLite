package org.mozilla.rocket.temp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TempInMemoryBookmarkRepository {

    private volatile static TempInMemoryBookmarkRepository instance;
    // From spec: allow duplicate and ordered, so using List rather than Set.
    private List<Bookmark> bookmarkList;
    private Map<UUID, Bookmark> bookmarkMap;

    private TempInMemoryBookmarkRepository() {
        bookmarkList = new ArrayList<>();
        bookmarkMap = new HashMap<>();
    }

    public boolean hasUrl(String url) {
        return bookmarkList.contains(new Bookmark(url));
    }

    synchronized public void add(String url, String name) {
        Bookmark candidate = new Bookmark(url, name);
        bookmarkList.add(candidate);
        bookmarkMap.put(candidate.uuid, candidate);
    }

    synchronized public void removeSingle(UUID uuid) {
        Bookmark target = bookmarkMap.get(uuid);
        int targetIndex = -1;
        for (int i = 0; i < bookmarkList.size() ; i++) {
            Bookmark candidate = bookmarkList.get(i);
            if (candidate.uuid.equals(target.uuid)) {
                targetIndex = i;
                break;
            }
        }
        bookmarkList.remove(targetIndex);
        bookmarkMap.remove(uuid);
    }

    // Remove duplicates as well
    synchronized public void removeAll(String url) {
        Bookmark target = new Bookmark(url);
        int indexOfItemToRemove = bookmarkList.indexOf(target);
        while (indexOfItemToRemove != -1) {
            Bookmark candidate = bookmarkList.get(indexOfItemToRemove);
            bookmarkList.remove(candidate);
            bookmarkMap.remove(candidate.uuid);
            indexOfItemToRemove = bookmarkList.indexOf(target);
        }
    }

    public List<Bookmark> list() {
        return Collections.unmodifiableList(bookmarkList);
    }

    public Bookmark get(UUID uuid) {
        return bookmarkMap.get(uuid);
    }

    public static TempInMemoryBookmarkRepository getInstance() {
        if (instance == null) {
            instance = new TempInMemoryBookmarkRepository();
        }
        return instance;
    }

    public static class Bookmark {

        private String address;
        private String name;
        private UUID uuid;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Bookmark)) {
                return false;
            }
            Bookmark bookmark = (Bookmark) o;
            return bookmark.address.equals(address);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] { address });
        }

        public Bookmark(String address, String name) {
            this.name = name;
            this.address = address;
            this.uuid = UUID.randomUUID();
        }

        public Bookmark(String address) {
            this(address, null);
        }

        public String getAddress() {
            return address;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}
