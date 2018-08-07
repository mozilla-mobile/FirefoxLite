package org.mozilla.rocket.banner;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONException;
import org.mozilla.focus.R;
import org.mozilla.rocket.glide.transformation.PorterDuffTransformation;

class FourSitesViewHolder extends BannerViewHolder {
    static final int VIEW_TYPE = 2;
    static final String VIEW_TYPE_NAME = "four_sites";
    private ViewGroup background;
    private OnClickListener onClickListener;
    private ImageView[] icons;
    private TextView[] textViews;

    FourSitesViewHolder(ViewGroup parent, OnClickListener onClickListener) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.banner2, parent, false));
        this.onClickListener = onClickListener;
        background = itemView.findViewById(R.id.banner_background);
        icons = new ImageView[]{itemView.findViewById(R.id.banner_icon_1),
                itemView.findViewById(R.id.banner_icon_2),
                itemView.findViewById(R.id.banner_icon_3),
                itemView.findViewById(R.id.banner_icon_4)};
        textViews = new TextView[]{itemView.findViewById(R.id.banner_label_1),
                itemView.findViewById(R.id.banner_label_2),
                itemView.findViewById(R.id.banner_label_3),
                itemView.findViewById(R.id.banner_label_4)};
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
            for (int i = 0; i < icons.length; i++) {
                PorterDuffColorFilter alphaToWhitePorterDuff = new PorterDuffColorFilter(context.getResources().getColor(R.color.sharedColorAppPaletteWhite), PorterDuff.Mode.DST_OVER);
                Glide.with(context)
                        .load(bannerDAO.values.getString(1 + i))
                        .apply(new RequestOptions().transforms(new PorterDuffTransformation(alphaToWhitePorterDuff), new CircleCrop()))
                        .into(icons[i]);
            }
            for (int i = 0; i < icons.length; i++) {
                textViews[i].setText(bannerDAO.values.getString(9 + i));
            }
        } catch (JSONException e) {
            // Invalid manifest
            e.printStackTrace();
        }
        for (int i = 0; i < icons.length; i++) {
            final int index = 5 + i;
            icons[i].setOnClickListener(v -> {
                try {
                    onClickListener.onClick(bannerDAO.values.getString(index));
                } catch (JSONException e) {
                    // Invalid manifest
                    e.printStackTrace();
                }
            });
        }
    }
}
