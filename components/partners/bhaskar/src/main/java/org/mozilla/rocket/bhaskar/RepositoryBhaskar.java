package org.mozilla.rocket.bhaskar;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.lite.partner.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RepositoryBhaskar extends Repository<BhaskarItem> {
    static final String SUBSCRIPTION_KEY_NAME = "bhaskar";
    static final String DEFAULT_SUBSCRIPTION_URL = "http://appfeed.bhaskar.com/webfeed/apidata/firefox?pageSize=%d&channel_slno=%d&pageNumber=%d";
    static final int FIRST_PAGE = 1;
    static final int DEFAULT_CHANNEL = 521;

    static Parser<BhaskarItem> PARSER = source -> {
        List<BhaskarItem> ret = new ArrayList<>();
        // TODO: 11/2/18 It takes 0.1s - 0.2s to create JsonObject, do we want to improve this?
        JSONObject root = new JSONObject(source);
        JSONObject data = root.getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        for (int i = 0 ; i < rows.length() ; i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.getString("id");
            String articleFrom = row.getString("articleFrom");
            String category = row.getString("category");
            String city = row.getString("city");
            String coverPic = new JSONArray(row.getString("coverPic")).getString(0);
            String description = row.getString("description");
            String detailUrl = row.getString("detailUrl");
            String keywords = row.getString("keywords");
            String language = row.getString("language");
            String province = row.getString("province");
            long publishTime = row.getLong("publishTime");
            String subcategory = row.getString("subcategory");
            String summary = row.getString("summary");
            String separator = "" + '\0';
            List<String> tags = Arrays.asList(row.getJSONArray("tags").join(separator).split(separator));
            String title = row.getString("title");
            BhaskarItem itemPojo = new BhaskarItem(id, coverPic, title, detailUrl, publishTime, summary, language, category, subcategory, keywords, description, tags, articleFrom, province, city);
            ret.add(itemPojo);
        }
        return ret;
    };

    public RepositoryBhaskar(Context context) {
        super(context, null, 3, null, null, SUBSCRIPTION_KEY_NAME, FIRST_PAGE, PARSER, true);
    }

    @Override
    protected String getSubscriptionUrl(int pageNumber) {
        return String.format(Locale.US, DEFAULT_SUBSCRIPTION_URL, DEFAULT_PAGE_SIZE, DEFAULT_CHANNEL, pageNumber);
    }
}
