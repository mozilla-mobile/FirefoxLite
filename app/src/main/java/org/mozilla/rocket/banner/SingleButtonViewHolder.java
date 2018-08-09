package org.mozilla.rocket.banner;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONException;
import org.mozilla.focus.R;

class SingleButtonViewHolder extends BannerViewHolder {
    static final int VIEW_TYPE = 1;
    static final String VIEW_TYPE_NAME = "single_button";
    static final int BUTTON_INDEX = 0;
    private ViewGroup background;
    private OnClickListener onClickListener;
    private TextView button;

    SingleButtonViewHolder(ViewGroup parent, OnClickListener onClickListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.banner1, parent, false));
        this.onClickListener = onClickListener;
        background = itemView.findViewById(R.id.banner_background);
        button = itemView.findViewById(R.id.button);
    }

    @Override
    public void onBindViewHolder(Context context, BannerDAO bannerDAO) {
        try {
            Glide.with(context).load(bannerDAO.values.getString(0)).into(new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                    background.setBackground(resource);
                }
            });
            button.setText(bannerDAO.values.getString(2));
        } catch (JSONException e) {
            // Invalid manifest
            e.printStackTrace();
        }
        button.setOnClickListener(v -> {
            try {
                sendClickItemTelemetry(bannerDAO.id, BUTTON_INDEX);
                onClickListener.onClick(bannerDAO.values.getString(1));
            } catch (JSONException e) {
                // Invalid manifest
                e.printStackTrace();
            }
        });
        background.setOnClickListener(v -> {
            sendClickBackgroundTelemetry(bannerDAO.id);
        });
    }
}
