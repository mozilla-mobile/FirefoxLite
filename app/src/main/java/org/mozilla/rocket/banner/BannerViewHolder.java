package org.mozilla.rocket.banner;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.mozilla.focus.telemetry.TelemetryWrapper;

import javax.annotation.Nullable;

public abstract class BannerViewHolder extends RecyclerView.ViewHolder  {
    protected @Nullable String id;

    BannerViewHolder(View itemView) {
        super(itemView);
    }

    // JSONException should never happen, see BannerAdapter's constructor.
    @CallSuper
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        id = bannerDAO.id;
    }

    protected final void sendClickItemTelemetry(int itemPosition) {
        if (id != null) {
            TelemetryWrapper.clickBannerItem(id, itemPosition);
        }
    }

    protected final void sendClickBackgroundTelemetry() {
        if (id != null) {
            TelemetryWrapper.clickBannerBackground(id);
        }
    }

    @Nullable
    public final String getId() {
        return id;
    }
}
