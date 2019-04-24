package org.mozilla.banner;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class BannerViewHolder extends RecyclerView.ViewHolder  {
    protected @Nullable String id;
    private TelemetryListener telemetryListener;

    BannerViewHolder(View itemView, TelemetryListener telemetryListener) {
        super(itemView);
        this.telemetryListener = telemetryListener;
    }

    // JSONException should never happen, see BannerAdapter's constructor.
    @CallSuper
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        id = bannerDAO.id;
    }

    final void sendClickItemTelemetry(int itemPosition) {
        if (id != null && telemetryListener != null) {
            telemetryListener.sendClickItemTelemetry(id, itemPosition);
        }
    }

    final void sendClickBackgroundTelemetry() {
        if (id != null && telemetryListener != null) {
            telemetryListener.sendClickBackgroundTelemetry(id);
        }
    }

    @Nullable
    public final String getId() {
        return id;
    }
}
