package org.mozilla.rocket.banner;

import org.json.JSONArray;

class BannerDAO {
    public static final String VERSION_KEY = "version";
    public static final String VALUES_KEY = "values";
    public static final String TYPE_KEY = "type";
    // To be used in the future
    // public int version;
    public JSONArray values;
    public String type;
}
