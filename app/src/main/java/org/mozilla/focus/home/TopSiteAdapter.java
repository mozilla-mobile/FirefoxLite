/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.FavIconUtils;

import java.util.ArrayList;
import java.util.List;

class TopSiteAdapter extends RecyclerView.Adapter<SiteViewHolder> {

    List<Site> sites = new ArrayList<>();
    final View.OnClickListener clickListener;
    final View.OnLongClickListener longClickListener;

    TopSiteAdapter(@NonNull List<Site> sites,
                   @Nullable View.OnClickListener clickListener,
                   @Nullable View.OnLongClickListener longClickListener) {
        this.sites.addAll(sites);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_site, parent, false);

        return new SiteViewHolder(view);
    }

    private int multiplyColorCodeByPercentage(int colorCode, float percentage) {
        int result = (int) ( colorCode * 1.1 ) ;
        if (result > 0xFF) {
            result = 0xFF;
        }
        return result;
    }

    @Override
    public void onBindViewHolder(SiteViewHolder holder, int position) {
        final Site site = sites.get(position);
        holder.text.setText(site.getTitle());
        if (site.getFavIcon() != null) {
            holder.img.setImageBitmap(site.getFavIcon());
            int dominantColor = FavIconUtils.getDominantColor(site.getFavIcon());
            int alpha = ( dominantColor & 0xFF000000 );
            int red = multiplyColorCodeByPercentage( ( dominantColor & 0x00FF0000 ) >> 16, 1.1f ) << 16;
            int green = multiplyColorCodeByPercentage( ( dominantColor & 0x0000FF00 ) >> 8, 1.1f ) << 8;
            int blue = multiplyColorCodeByPercentage( ( dominantColor & 0x000000FF ), 1.1f );
            ((View)holder.img.getParent()).setBackgroundColor(alpha + red + green + blue);
        } else {
            //need default icon?
            holder.img.setImageBitmap(null);
        }

        // let click listener knows which site is clicked
        holder.itemView.setTag(site);

        if (clickListener != null) {
            holder.itemView.setOnClickListener(clickListener);
        }
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return sites.size();
    }

    public void addSite(int index, @NonNull Site toAdd) {
       this.sites.add(index, toAdd);
        notifyItemInserted(index);
    }

    public void removeSite(@NonNull Site toRemove) {
        for (int i = 0; i < this.sites.size(); i++) {
            final Site site = this.sites.get(i);
            if (site.getId() == toRemove.getId()) {
                this.sites.remove(i);
                notifyDataSetChanged();
//                notifyItemRemoved(i);
            }
        }
    }

    public void setSites(@NonNull List<Site> sites) {
        this.sites = sites;
        notifyDataSetChanged();
    }
}
