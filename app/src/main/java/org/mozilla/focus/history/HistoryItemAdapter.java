/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.history;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.ItemClosingPanelFragmentStatusListener;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.history.model.DateSection;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.site.SiteItemViewHolder;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.rocket.content.HomeFragmentViewState;
import org.mozilla.threadutils.ThreadUtils;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.icon.FavIconUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by joseph on 08/08/2017.
 */

public class HistoryItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener,
        QueryHandler.AsyncQueryListener, QueryHandler.AsyncDeleteListener {

    private static final int VIEW_TYPE_SITE = 1;
    private static final int VIEW_TYPE_DATE = 2;

    private static final int PAGE_SIZE = 50;

    private List mItems = new ArrayList();
    private RecyclerView mRecyclerView;
    private Context mContext;
    private ItemClosingPanelFragmentStatusListener mHistoryListener;
    private boolean mIsInitialQuery;
    private boolean mIsLoading;
    private boolean mIsLastPage;
    private int mCurrentCount;

    public HistoryItemAdapter(RecyclerView recyclerView, Context context, ItemClosingPanelFragmentStatusListener historyListener) {
        mRecyclerView = recyclerView;
        mContext = context;
        mHistoryListener = historyListener;
        mIsInitialQuery = true;
        notifyStatusListener(BrowsingHistoryFragment.ON_OPENING);
        loadMoreItems();
    }

    public void tryLoadMore() {
        if (!mIsLoading && !mIsLastPage) {
            loadMoreItems();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SITE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_website, parent, false);
            return new SiteItemViewHolder(v);
        } else if (viewType == VIEW_TYPE_DATE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_date, parent, false);
            return new DateItemViewHolder(v);
        }
        return null;
    }

    private void setImageViewWithDefaultBitmap(ImageView imageView, String url) {
        imageView.setImageBitmap(DimenUtils.getInitialBitmap(imageView.getResources(), FavIconUtils.getRepresentativeCharacter(url), Color.WHITE));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof SiteItemViewHolder) {
            final Site item = (Site) mItems.get(position);

            if (item != null) {
                final SiteItemViewHolder siteVH = (SiteItemViewHolder) holder;
                siteVH.rootView.setOnClickListener(this);
                siteVH.textMain.setText(item.getTitle());
                siteVH.textSecondary.setText(item.getUrl());
                String favIconUri = item.getFavIconUri();
                if (favIconUri != null) {
                    Glide.with(siteVH.imgFav.getContext())
                            .asBitmap()
                            .load(favIconUri)
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                    if (DimenUtils.iconTooBlurry(siteVH.imgFav.getResources(), resource.getWidth())) {
                                        setImageViewWithDefaultBitmap(siteVH.imgFav, item.getUrl());
                                    } else {
                                        siteVH.imgFav.setImageBitmap(resource);
                                    }
                                }
                            });
                } else {
                    setImageViewWithDefaultBitmap(siteVH.imgFav, item.getUrl());
                }

                final PopupMenu popupMenu = new PopupMenu(mContext, siteVH.btnMore);
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.browsing_history_menu_delete) {
                        BrowsingHistoryManager.getInstance().delete(item.getId(), HistoryItemAdapter.this);
                        TelemetryWrapper.historyRemoveLink();
                        // Delete favicon
                        String uriString = item.getFavIconUri();
                        if (uriString == null) {
                            return false;
                        }
                        final URI fileUri;
                        try {
                            fileUri = new URI(uriString);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            return false;
                        }
                        final Runnable runnable = new FileUtils.DeleteFileRunnable(new File(fileUri));
                        ThreadUtils.postToBackgroundThread(runnable);
                    }
                    return false;
                });
                popupMenu.inflate(R.menu.menu_browsing_history_option);

                siteVH.btnMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMenu.show();
                        TelemetryWrapper.showHistoryContextMenu();
                    }
                });

            }
        } else if (holder instanceof DateItemViewHolder) {
            final DateSection item = (DateSection) mItems.get(position);

            if (item != null) {
                final DateItemViewHolder dateVH = (DateItemViewHolder) holder;
                dateVH.textDate.setText(DateUtils.getRelativeTimeSpanString(item.getTimestamp(), System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems.get(position) instanceof DateSection) {
            return VIEW_TYPE_DATE;
        } else {
            return VIEW_TYPE_SITE;
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onClick(View v) {
        final int position = mRecyclerView.getChildAdapterPosition(v);
        if (position != RecyclerView.NO_POSITION && position < mItems.size()) {
            Object item = mItems.get(position);
            if (item instanceof Site && mContext instanceof FragmentListener) {
                ScreenNavigator.get(mContext).showBrowserScreen(((Site) item).getUrl(), true, false);
                mHistoryListener.onItemClicked();
                HomeFragmentViewState.reset();
                TelemetryWrapper.historyOpenLink();
            }
        }
    }

    @Override
    public void onQueryComplete(List result) {
        mIsLastPage = result.size() == 0;
        if (mIsInitialQuery) {
            mIsInitialQuery = false;
        }
        for (Object site : result) {
            add(site);
        }

        if (mItems.size() > 0) {
            notifyStatusListener(BrowsingHistoryFragment.VIEW_TYPE_NON_EMPTY);
        } else {
            notifyStatusListener(BrowsingHistoryFragment.VIEW_TYPE_EMPTY);
        }
        mIsLoading = false;
    }

    @Override
    public void onDeleteComplete(int result, long id) {
        if (result > 0) {
            if (id < 0) {
                final int count = mItems.size();
                mItems.clear();
                notifyItemRangeRemoved(0, count);
                notifyStatusListener(BrowsingHistoryFragment.VIEW_TYPE_EMPTY);
            } else {
                remove(getItemPositionById(id));
                if (mItems.size() == 0) {
                    notifyStatusListener(BrowsingHistoryFragment.VIEW_TYPE_EMPTY);
                }
            }
        }
    }

    public void clear() {
        final Runnable runnable = new FileUtils.DeleteFolderRunnable(FileUtils.getFaviconFolder(mContext));
        ThreadUtils.postToBackgroundThread(runnable);
        BrowsingHistoryManager.getInstance().deleteAll(this);
    }

    private void add(Object item) {
        if (mItems.size() > 0 && isSameDay(((Site) mItems.get(mItems.size() - 1)).getLastViewTimestamp(), ((Site) item).getLastViewTimestamp())) {
            mItems.add(item);
            notifyItemInserted(mItems.size());
        } else {
            mItems.add(new DateSection(((Site) item).getLastViewTimestamp()));
            mItems.add(item);
            notifyItemRangeInserted(mItems.size() - 2, 2);
        }
        ++mCurrentCount;
    }

    private void remove(int position) {
        if (position < 0 || position >= mItems.size()) {
            return;
        }

        Object previous = position == 0 ? null : mItems.get(position - 1);
        Object next = (position + 1) == mItems.size() ? null : mItems.get(position + 1);
        if (previous instanceof Site || next instanceof Site) {
            mItems.remove(position);
            notifyItemRemoved(position);
        } else {
            mItems.remove(position);
            mItems.remove(position - 1);
            notifyItemRangeRemoved(position - 1, 2);
        }
        --mCurrentCount;
    }

    private void loadMoreItems() {
        mIsLoading = true;
        BrowsingHistoryManager.getInstance().query(mCurrentCount, PAGE_SIZE - (mCurrentCount % PAGE_SIZE), this);
    }

    private void notifyStatusListener(@PanelFragment.ViewStatus int status) {
        if (mHistoryListener != null) {
            mHistoryListener.onStatus(status);
        }
    }

    private int getItemPositionById(long id) {
        for (int i = 0; i < mItems.size(); i++) {
            Object item = mItems.get(i);
            if (item instanceof Site) {
                if (id == ((Site) item).getId()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isSameDay(long day1, long day2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(day1);
        cal2.setTimeInMillis(day2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static class DateItemViewHolder extends RecyclerView.ViewHolder {

        private TextView textDate;

        public DateItemViewHolder(View itemView) {
            super(itemView);
            textDate = (TextView) itemView.findViewById(R.id.history_item_date);
        }

    }
}
