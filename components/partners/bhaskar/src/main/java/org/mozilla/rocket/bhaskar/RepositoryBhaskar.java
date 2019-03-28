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
    static final int FIRST_PAGE = 1;
    static final int DEFAULT_CHANNEL = 521;

    static Parser<BhaskarItem> PARSER = source -> {
        List<BhaskarItem> ret = new ArrayList<>();
        // TODO: 11/2/18 It takes 0.1s - 0.2s to create JsonObject, do we want to improve this?
        JSONObject root = new JSONObject(source);
        JSONObject data = root.getJSONObject("data");
        JSONArray rows = data.getJSONArray("rows");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.optString("id", null);
            String articleFrom = row.optString("articleFrom", null);
            String category = row.optString("category", null);
            String city = row.optString("city", null);
            final String coverPics = row.optString("coverPic", null);
            String coverPic = null;
            if (coverPics != null) {
                final JSONArray picsArray = new JSONArray(coverPics);
                if (picsArray.length() > 0) {
                    coverPic = picsArray.getString(0);
                }
            }
            String description = row.optString("description", null);
            String detailUrl = row.optString("detailUrl", null);
            String keywords = row.optString("keywords", null);
            String language = row.optString("language", null);
            String province = row.optString("province", null);
            long publishTime = row.optLong("publishTime", -1L);
            String subcategory = row.optString("subcategory", null);
            String summary = row.optString("summary", null);
            String separator = "" + '\0';
            List<String> tags = Arrays.asList(row.getJSONArray("tags").join(separator).split(separator));
            String title = row.getString("title");
            if (id == null || title == null || detailUrl == null || publishTime == -1L) {
                // skip this item
                continue;
            }
            BhaskarItem itemPojo = new BhaskarItem(id, coverPic, title, detailUrl, publishTime, summary, language, category, subcategory, keywords, description, tags, articleFrom, province, city);
            ret.add(itemPojo);
        }
        return ret;
    };

    public RepositoryBhaskar(Context context, String subscriptionUrl) {
        super(context, null, 3, null, null, SUBSCRIPTION_KEY_NAME, subscriptionUrl, FIRST_PAGE, PARSER, true);
    }

    @Override
    protected String getSubscriptionUrl(int pageNumber) {
        return String.format(Locale.US, subscriptionUrl, DEFAULT_PAGE_SIZE, DEFAULT_CHANNEL, pageNumber);
    }
}
