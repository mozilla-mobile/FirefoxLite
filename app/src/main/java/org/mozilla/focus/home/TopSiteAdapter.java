/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.rocket.home.pinsite.PinSiteManager;
import org.mozilla.strictmodeviolator.StrictModeViolation;

import java.util.ArrayList;
import java.util.List;

class TopSiteAdapter extends RecyclerView.Adapter<SiteViewHolder> {

    private List<Site> sites = new ArrayList<>();
    private final View.OnClickListener clickListener;
    private final View.OnLongClickListener longClickListener;
    private final PinSiteManager pinSiteManager;

    TopSiteAdapter(@NonNull List<Site> sites,
                   @Nullable View.OnClickListener clickListener,
                   @Nullable View.OnLongClickListener longClickListener,
                   @NonNull PinSiteManager pinSiteManager) {
        this.sites.addAll(sites);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.pinSiteManager = pinSiteManager;
    }

    @NotNull
    @Override
    public SiteViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_site, parent, false);

        return new SiteViewHolder(view);
    }

    private int addWhiteToColorCode(int colorCode, @SuppressWarnings("SameParameterValue") float percentage) {
        int result = (int) (colorCode + 0xFF * percentage / 2);
        if (result > 0xFF) {
            result = 0xFF;
        }
        return result;
    }

    @Override
    public void onBindViewHolder(@NotNull SiteViewHolder holder, int position) {
        final Site site = sites.get(position);
        holder.text.setText(site.getTitle());

        // Tried AsyncTask and other simple offloading, the performance drops significantly.
        // FIXME: 9/21/18 by saving bitmap color, cause FaviconUtils.getDominantColor runs slow.
        // Favicon
        Bitmap favicon = StrictModeViolation.tempGrant(StrictMode.ThreadPolicy.Builder::permitDiskReads, () -> {
            return getFavicon(holder.itemView.getContext(), site);
        });
        holder.img.setVisibility(View.VISIBLE);
        holder.img.setImageBitmap(favicon);

        // Background color
        int backgroundColor = calculateBackgroundColor(favicon);
        ViewCompat.setBackgroundTintList(holder.img, ColorStateList.valueOf(backgroundColor));

        // Pin
        holder.pinView.setVisibility(pinSiteManager.isPinned(site) ? View.VISIBLE : View.GONE);
        holder.pinView.setPinColor(backgroundColor);

        // let click listener knows which site is clicked
        holder.itemView.setTag(site);

        if (clickListener != null) {
            holder.itemView.setOnClickListener(clickListener);
        }
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(longClickListener);
        }
    }

    private Bitmap getFavicon(Context context, Site site) {
        String faviconUri = site.getFavIconUri();
        Bitmap favicon = null;
        if (faviconUri != null) {
            favicon = FavIconUtils.getBitmapFromUri(context, faviconUri);
        }

        return getBestFavicon(context.getResources(), site.getUrl(), favicon);
    }

    private Bitmap getBestFavicon(Resources res, String url, @Nullable Bitmap favicon) {
        if (favicon == null) {
            return createFavicon(res, url, Color.WHITE);
        } else if (DimenUtils.iconTooBlurry(res, favicon.getWidth())) {
            return createFavicon(res, url, FavIconUtils.getDominantColor(favicon));
        } else {
            return favicon;
        }
    }

    private Bitmap createFavicon(Resources resources, String url, int backgroundColor) {
        return DimenUtils.getInitialBitmap(resources, FavIconUtils.getRepresentativeCharacter(url),
                backgroundColor);
    }

    private int calculateBackgroundColor(Bitmap favicon) {
        int dominantColor = FavIconUtils.getDominantColor(favicon);
        int alpha = (dominantColor & 0xFF000000);
        // Add 25% white to dominant Color
        int red = addWhiteToColorCode((dominantColor & 0x00FF0000) >> 16, 0.25f) << 16;
        int green = addWhiteToColorCode((dominantColor & 0x0000FF00) >> 8, 0.25f) << 8;
        int blue = addWhiteToColorCode((dominantColor & 0x000000FF), 0.25f);
        return alpha + red + green + blue;
    }

    @Override
    public int getItemCount() {
        return sites.size();
    }

    void addSite(int index, @NonNull Site toAdd) {
        this.sites.add(index, toAdd);
        notifyItemInserted(index);
    }

    public void setSites(@NonNull List<Site> sites) {
        this.sites = sites;
        notifyDataSetChanged();
    }
}
