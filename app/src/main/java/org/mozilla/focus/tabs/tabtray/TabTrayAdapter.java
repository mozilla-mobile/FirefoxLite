/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.tabs.Tab;

import java.util.List;

public class TabTrayAdapter extends RecyclerView.Adapter<TabTrayAdapter.ViewHolder> {

    private List<Tab> tabs;
    private int focusedTabPosition = -1;

    private TabClickListener tabClickListener;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_tab_tray, parent, false));

        InternalTabClickListener listener = new InternalTabClickListener(holder, tabClickListener);

        holder.itemView.setOnClickListener(listener);
        holder.closeButton.setOnClickListener(listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemView.setSelected(position == this.focusedTabPosition);

        Resources resources = holder.itemView.getResources();
        Tab tab = tabs.get(position);

        String title = tab.getTitle();
        holder.websiteTitle.setText(TextUtils.isEmpty(title) ?
                resources.getString(R.string.app_name) : title);

        holder.websiteSubtitle.setText(tab.getUrl());
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    int getItemPosition(Tab tab) {
        return tabs.indexOf(tab);
    }

    void setTabClickListener(TabClickListener tabClickListener) {
        this.tabClickListener = tabClickListener;
    }

    void setData(List<Tab> tabs) {
        this.tabs = tabs;
    }

    void setFocusedTab(int tabPosition) {
        this.focusedTabPosition = tabPosition;
    }

    int getFocusedTabPosition() {
        return this.focusedTabPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView websiteTitle;
        TextView websiteSubtitle;
        View closeButton;
        //ImageView websiteIcon;

        ViewHolder(View itemView) {
            super(itemView);
            websiteTitle = itemView.findViewById(R.id.website_title);
            websiteSubtitle = itemView.findViewById(R.id.website_subtitle);
            closeButton = itemView.findViewById(R.id.close_button);
            //websiteIcon = itemView.findViewById(R.id.website_icon);
        }
    }

    static class InternalTabClickListener implements View.OnClickListener {
        private ViewHolder holder;
        private TabClickListener tabClickListener;

        InternalTabClickListener(ViewHolder holder, TabClickListener tabClickListener) {
            this.holder = holder;
            this.tabClickListener = tabClickListener;
        }

        @Override
        public void onClick(View v) {
            if (tabClickListener == null) {
                return;
            }

            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                dispatchOnClick(v, pos);
            }
        }

        private void dispatchOnClick(View v, int position) {
            switch (v.getId()) {
                case R.id.root_view:
                    tabClickListener.onTabClick(position);
                    break;

                case R.id.close_button:
                    tabClickListener.onTabCloseClick(position);
                    break;

                default:
                    break;
            }
        }
    }

    public interface TabClickListener {
        void onTabClick(int tabPosition);
        void onTabCloseClick(int tabPosition);
    }
}
