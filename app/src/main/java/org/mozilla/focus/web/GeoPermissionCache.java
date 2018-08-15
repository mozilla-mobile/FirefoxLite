package org.mozilla.focus.web;

import java.util.HashMap;
import java.util.Map;

public class GeoPermissionCache {

    private static final Map<String, Boolean> permissions = new HashMap<>();

    public static Boolean getAllowed(String origin) {
        return permissions.get(origin);
    }

    public static void putAllowed(String origin, Boolean allowed) {
        permissions.put(origin, allowed);
    }

    public static void clear() {
        permissions.clear();
    }
}
