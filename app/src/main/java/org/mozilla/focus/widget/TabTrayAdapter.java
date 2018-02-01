/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.tabs.Tab;

public class TabTrayAdapter extends RecyclerView.Adapter<TabTrayAdapter.ViewHolder> {

    private TabTrayContract.Model model;
    private TabClickListener tabClickListener;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_tab_tray, parent, false));

        InternalTabClickListener listener = new InternalTabClickListener(holder,
                tabClickListener, model);

        holder.itemView.setOnClickListener(listener);
        holder.closeButton.setOnClickListener(listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemView.setSelected(position == model.getCurrentTabPosition());

        Tab tab = model.getTabs().get(position);
        holder.websiteTitle.setText(tab.getTitle());
        holder.websiteSubtitle.setText(tab.getUrl());
    }

    @Override
    public int getItemCount() {
        return this.model.getTabCount();
    }

    void setTabClickListener(TabClickListener tabClickListener) {
        this.tabClickListener = tabClickListener;
    }

    void setDataModel(TabTrayContract.Model model) {
        this.model = model;
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
        private TabTrayContract.Model model;
        private ViewHolder holder;
        private TabClickListener tabClickListener;

        InternalTabClickListener(ViewHolder holder, TabClickListener tabClickListener,
                                 TabTrayContract.Model model) {
            this.holder = holder;
            this.tabClickListener = tabClickListener;
            this.model = model;
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
                    tabClickListener.onItemClick(model.getTabs().get(position));
                    break;

                case R.id.close_button:
                    tabClickListener.onCloseClick(model.getTabs().get(position));
                    break;

                default:
                    break;
            }
        }
    }

    public interface TabClickListener {
        void onItemClick(Tab tab);
        void onCloseClick(Tab tab);
    }

    public static class TabClickAdapter implements TabClickListener {

        @Override
        public void onItemClick(Tab tab) {

        }

        @Override
        public void onCloseClick(Tab tab) {

        }
    }
}
