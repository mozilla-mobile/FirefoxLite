package org.mozilla.rocket.banner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.mozilla.focus.R;

class BasicViewHolder extends BannerViewHolder {
    static final int VIEW_TYPE = 0;
    static final String VIEW_TYPE_NAME = "basic";
    private ImageView background;
    private OnClickListener onClickListener;

    BasicViewHolder(ViewGroup parent, OnClickListener onClickListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.banner0, parent, false));
        this.onClickListener = onClickListener;
        background = itemView.findViewById(R.id.banner_background);
    }

    @Override
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        try {
            Glide.with(context).load(bannerDAO.values.getString(0)).into(background);
        } catch (JSONException e) {
            // Invalid manifest
            e.printStackTrace();
        }
        background.setOnClickListener(v -> {
            sendClickBackgroundTelemetry(bannerDAO.id);
            try {
                onClickListener.onClick(bannerDAO.values.getString(1));
            } catch (JSONException e) {
                // Invalid manifest
                e.printStackTrace();
            }
        });
    }
}
