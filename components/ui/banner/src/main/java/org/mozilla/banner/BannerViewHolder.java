package org.mozilla.banner;

import android.content.Context;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BannerViewHolder extends RecyclerView.ViewHolder  {
    protected @Nullable String id;
    private @Nullable JSONObject jsonObject;
    private TelemetryListener telemetryListener;

    BannerViewHolder(View itemView, TelemetryListener telemetryListener) {
        super(itemView);
        this.telemetryListener = telemetryListener;
    }

    // JSONException should never happen, see BannerAdapter's constructor.
    @CallSuper
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        id = bannerDAO.id;
        this.jsonObject = createTelemetryBase(bannerDAO);
        try {
            addTelemetryData(bannerDAO, jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    final void sendClickItemTelemetry(int itemPosition) {
        if (jsonObject != null && telemetryListener != null) {
            telemetryListener.sendClickItemTelemetry(jsonObject.toString(), itemPosition);
        }
    }

    final void sendClickBackgroundTelemetry() {
        if (jsonObject != null && telemetryListener != null) {
            telemetryListener.sendClickBackgroundTelemetry(jsonObject.toString());
        }
    }

    private JSONObject createTelemetryBase(BannerDAO bannerDAO) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", bannerDAO.id == null ? "-1" : bannerDAO.id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    protected void addTelemetryData(BannerDAO bannerDAO, JSONObject jsonObject) throws JSONException {

    }

    @Nullable
    public final String getId() {
        return id;
    }
}
