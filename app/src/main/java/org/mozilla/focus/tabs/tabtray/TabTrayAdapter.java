/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.rocket.nightmode.themed.ThemedRecyclerView;
import org.mozilla.rocket.nightmode.themed.ThemedRelativeLayout;
import org.mozilla.rocket.nightmode.themed.ThemedTextView;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.rocket.nightmode.themed.ThemedView;
import org.mozilla.rocket.tabs.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TabTrayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SHOPPING_SEARCH = 1;
    private static final int VIEW_TYPE_TAB = 2;

    private boolean showShoppingSearch;
    private String keyword;

    private List<Session> tabs = new ArrayList<>();
    private Session focusedTab;

    private TabClickListener tabClickListener;

    private RequestManager requestManager;

    private HashMap<String, Drawable> localIconCache = new HashMap<>();

    private boolean isNight;

    TabTrayAdapter(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_SHOPPING_SEARCH: {
                ShoppingSearchViewHolder holder = new ShoppingSearchViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_shopping_search, parent, false));

                InternalTabClickListener listener = new InternalTabClickListener(holder, tabClickListener);

                holder.itemView.setOnClickListener(listener);
                holder.closeButton.setOnClickListener(listener);
                return holder;
            }
            case VIEW_TYPE_TAB: {
                TabViewHolder holder = new TabViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_tab_tray, parent, false));

                InternalTabClickListener listener = new InternalTabClickListener(holder, tabClickListener);

                holder.itemView.setOnClickListener(listener);
                holder.closeButton.setOnClickListener(listener);
                return holder;
            }
            default:
                // unknown view type
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Resources resources = holder.itemView.getResources();
        switch (getItemViewType(position)) {
            case VIEW_TYPE_SHOPPING_SEARCH: {
                ShoppingSearchViewHolder ssHolder = (ShoppingSearchViewHolder) holder;
                String title = TextUtils.isEmpty(keyword) ? resources.getString(R.string.shopping_switch_ui_tabtray_title_no_keyword) :
                        resources.getString(R.string.shopping_switch_ui_tabtray_title, keyword);
                ssHolder.title.setText(title);
                ssHolder.closeIcon.setNightMode(isNight);
                break;
            }
            case VIEW_TYPE_TAB: {
                Session tab = tabs.get(showShoppingSearch ? position - 1 : position);
                TabViewHolder tabHolder = (TabViewHolder) holder;
                tabHolder.itemView.setSelected(tab == focusedTab);

                String title = getTitle(tab, tabHolder);
                tabHolder.websiteTitle.setText(TextUtils.isEmpty(title) ?
                        resources.getString(R.string.app_name) : title);

                String url = tab.getUrl();
                if (!TextUtils.isEmpty(url)) {
                    tabHolder.websiteSubtitle.setText(tab.getUrl());
                }

                setFavicon(tab, tabHolder);
                tabHolder.rootView.setNightMode(isNight);
                tabHolder.websiteTitle.setNightMode(isNight);
                tabHolder.websiteSubtitle.setNightMode(isNight);
                tabHolder.closeIcon.setNightMode(isNight);
                break;
            }
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof TabViewHolder) {
            TabViewHolder tabHolder = (TabViewHolder) holder;
            tabHolder.websiteTitle.setText("");
            tabHolder.websiteSubtitle.setText("");
            updateFavicon(tabHolder, null);
        }
    }

    @Override
    public int getItemCount() {
        return showShoppingSearch ? tabs.size() + 1 : tabs.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (showShoppingSearch && position == 0) ? VIEW_TYPE_SHOPPING_SEARCH : VIEW_TYPE_TAB;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (recyclerView instanceof ThemedRecyclerView) {
            ThemedRecyclerView themedRecyclerView = (ThemedRecyclerView) recyclerView;
            this.isNight = themedRecyclerView.isNightMode();
        }
    }

    void setTabClickListener(TabClickListener tabClickListener) {
        this.tabClickListener = tabClickListener;
    }

    void setShoppingSearch(boolean showShoppingSearch, String keyword) {
        this.showShoppingSearch = showShoppingSearch;
        this.keyword = keyword;
    }

    void setData(List<Session> tabs) {
        this.tabs.clear();
        this.tabs.addAll(tabs);
    }

    List<Session> getData() {
        return this.tabs;
    }

    void setFocusedTab(Session tab) {
        focusedTab = tab;
    }

    Session getFocusedTab() {
        return focusedTab;
    }

    private String getTitle(Session tab, TabViewHolder holder) {
        String newTitle = tab.getTitle();
        String currentTitle = String.valueOf(holder.websiteTitle.getText());

        if (TextUtils.isEmpty(newTitle)) {
            return TextUtils.isEmpty(currentTitle) ? "" : currentTitle;
        }

        return newTitle;
    }

    private void setFavicon(Session tab, final TabViewHolder holder) {
        String uri = tab.getUrl();
        if (TextUtils.isEmpty(uri)) {
            return;
        }

        loadCachedFavicon(tab, holder);
    }

    private void loadCachedFavicon(final Session tab, final TabViewHolder holder) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .dontAnimate();

        Bitmap favicon = tab.getFavicon();
        FaviconModel model = new FaviconModel(tab.getUrl(),
                DimenUtils.getFavIconType(holder.itemView.getResources(), favicon),
                favicon);

        requestManager
                .load(model)
                .apply(options)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target,
                                                boolean isFirstResource) {
                        loadGeneratedFavicon(tab, holder);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target,
                                                   DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource,
                                                Transition<? super Drawable> transition) {
                        updateFavicon(holder, resource);
                    }
                });
    }

    private void loadGeneratedFavicon(Session tab, final TabViewHolder holder) {
        Character symbol = FavIconUtils.getRepresentativeCharacter(tab.getUrl());
        Bitmap favicon = tab.getFavicon();
        int backgroundColor = (favicon == null) ? Color.WHITE : FavIconUtils.getDominantColor(favicon);
        String key = symbol.toString() + "_" + Integer.toHexString(backgroundColor);

        if (localIconCache.containsKey(key)) {
            updateFavicon(holder, localIconCache.get(key));
        } else {
            BitmapDrawable drawable = new BitmapDrawable(holder.itemView.getResources(),
                    DimenUtils.getInitialBitmap(holder.itemView.getResources(), symbol, backgroundColor));
            localIconCache.put(key, drawable);
            updateFavicon(holder, drawable);
        }
    }

    private void updateFavicon(TabViewHolder holder, @Nullable Drawable drawable) {
        if (drawable != null) {
            holder.websiteIcon.setImageDrawable(drawable);
            holder.websiteIcon.setBackgroundColor(Color.TRANSPARENT);
        } else {
            holder.websiteIcon.setImageResource(R.drawable.favicon_default);
            holder.websiteIcon.setBackgroundColor(ContextCompat.getColor(
                    holder.websiteIcon.getContext(),
                    R.color.tabTrayItemIconBackground));
        }
    }

    static class ShoppingSearchViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        View closeButton;
        ThemedView closeIcon;

        ShoppingSearchViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            closeButton = itemView.findViewById(R.id.close_button);
            closeIcon = itemView.findViewById(R.id.close_icon);
        }
    }

    class TabViewHolder extends RecyclerView.ViewHolder {
        ThemedRelativeLayout rootView;
        ThemedTextView websiteTitle;
        ThemedTextView websiteSubtitle;
        View closeButton;
        ImageView websiteIcon;
        ThemedView closeIcon;

        TabViewHolder(View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.root_view);
            websiteTitle = itemView.findViewById(R.id.website_title);
            websiteSubtitle = itemView.findViewById(R.id.website_subtitle);
            closeButton = itemView.findViewById(R.id.close_button);
            websiteIcon = itemView.findViewById(R.id.website_icon);
            closeIcon = itemView.findViewById(R.id.close_icon);
        }

        public int getOriginPosition() {
            final int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return position;
            } else {
                return showShoppingSearch ? position - 1 : position;
            }
        }
    }

    static class InternalTabClickListener implements View.OnClickListener {
        private static final int POSITION_OF_SHOPPING_SEARCH = -99;

        private RecyclerView.ViewHolder holder;
        private TabClickListener tabClickListener;

        InternalTabClickListener(RecyclerView.ViewHolder holder, TabClickListener tabClickListener) {
            this.holder = holder;
            this.tabClickListener = tabClickListener;
        }

        @Override
        public void onClick(View v) {
            if (tabClickListener == null) {
                return;
            }

            if (holder instanceof ShoppingSearchViewHolder) {
                dispatchOnClick(v, POSITION_OF_SHOPPING_SEARCH);
            } else if (holder instanceof  TabViewHolder) {
                int pos = ((TabViewHolder) holder).getOriginPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    dispatchOnClick(v, pos);
                }
            }
        }

        private void dispatchOnClick(View v, int position) {
            switch (v.getId()) {
                case R.id.root_view:
                    if (position == POSITION_OF_SHOPPING_SEARCH) {
                        tabClickListener.onShoppingSearchClick();
                    } else {
                        tabClickListener.onTabClick(position);
                    }
                    break;

                case R.id.close_button:
                    if (position == POSITION_OF_SHOPPING_SEARCH) {
                        tabClickListener.onShoppingSearchCloseClick();
                    } else {
                        tabClickListener.onTabCloseClick(position);
                    }
                    break;

                default:
                    break;
            }
        }
    }

    public interface TabClickListener {
        void onShoppingSearchClick();

        void onShoppingSearchCloseClick();

        void onTabClick(int tabPosition);

        void onTabCloseClick(int tabPosition);
    }
}
