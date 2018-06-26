package org.mozilla.focus.site;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;

public class SiteItemViewHolder extends RecyclerView.ViewHolder {
    public ViewGroup rootView;
    public ImageView imgFav;
    public TextView textMain, textSecondary;
    public FrameLayout btnMore;

    public SiteItemViewHolder(View itemView) {
        super(itemView);
        rootView = itemView.findViewById(R.id.history_item_root_view);
        imgFav =  itemView.findViewById(R.id.history_item_img_fav);
        textMain =  itemView.findViewById(R.id.history_item_text_main);
        textSecondary = itemView.findViewById(R.id.history_item_text_secondary);
        btnMore = itemView.findViewById(R.id.history_item_btn_more);
    }
}
