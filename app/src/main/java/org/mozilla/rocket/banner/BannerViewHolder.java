package org.mozilla.rocket.banner;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.mozilla.focus.telemetry.TelemetryWrapper;

public abstract class BannerViewHolder extends RecyclerView.ViewHolder  {

    BannerViewHolder(View itemView) {
        super(itemView);
    }

    // JSONException should never happen, see BannerAdapter's constructor.
    public abstract void onBindViewHolder(Context context, BannerDAO bannerDAO);

    public void sendClickItemTelemetry(String pageId, int itemPosition) {
        TelemetryWrapper.clickBannerItem(pageId, itemPosition);
    }

    public void sendClickBackgroundTelemetry(String pageId) {
        TelemetryWrapper.clickBannerBackground(pageId);
    }
}
