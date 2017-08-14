package org.mozilla.focus.history;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.history.model.DateSection;
import org.mozilla.focus.history.model.Site;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by joseph on 08/08/2017.
 */

public class HistoryItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener,
        BrowsingHistoryManager.AsyncQueryListener, BrowsingHistoryManager.AsyncDeleteListener {

    private static final int VIEW_TYPE_SITE = 1;
    private static final int VIEW_TYPE_DATE = 2;

    private static final int PAGE_SIZE = 20;

    private List mItems = new ArrayList();
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private Context mContext;
    private boolean mIsInitialQuery;
    private boolean mIsLoading;
    private boolean mIsLastPage;
    private int mCurrentCount;

    public interface EmptyListener {
        void onEmpty(boolean flag);
    }

    public HistoryItemAdapter(RecyclerView recyclerView, Context context, LinearLayoutManager layoutManager) {
        mRecyclerView = recyclerView;
        mContext = context;
        mLayoutManager = layoutManager;
        mIsInitialQuery = true;
        loadMoreItems();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                final int visibleItemCount = mLayoutManager.getChildCount();
                final int totalItemCount = mLayoutManager.getItemCount();
                final int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
                if (!mIsLoading && !mIsLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        loadMoreItems();
                    }
                }
            }
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SITE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_website, parent, false);
            return new SiteItemViewHolder(v);
        } else if(viewType == VIEW_TYPE_DATE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_date, parent, false);
            return new DateItemViewHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if(holder instanceof SiteItemViewHolder) {
            final Site item = (Site) mItems.get(position);

            if(item != null) {
                final SiteItemViewHolder siteVH = (SiteItemViewHolder) holder;
                siteVH.rootView.setOnClickListener(this);
                siteVH.textMain.setText(item.getTitle());
                siteVH.textSecondary.setText(item.getUrl());
                siteVH.imgFav.setImageBitmap(item.getFavIcon());

                final PopupMenu popupMenu = new PopupMenu(mContext, siteVH.btnMore);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if(menuItem.getItemId() == R.id.browsing_history_menu_delete) {
                            BrowsingHistoryManager.getInstance().delete(item.getId(), HistoryItemAdapter.this);
                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.menu_browsing_history_option);

                siteVH.btnMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupMenu.show();
                    }
                });

            }
        } else if(holder instanceof DateItemViewHolder) {
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
        int position = mRecyclerView.getChildLayoutPosition(v);
        Site site = (Site)mItems.get(position);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.setData(Uri.parse(site.getUrl()));
        intent.setAction(Intent.ACTION_VIEW);
        mContext.startActivity(intent);
        if(mContext instanceof BrowsingHistoryActivity) {
            ((Activity) mContext).finishAndRemoveTask();
        }
    }

    @Override
    public void onQueryComplete(List sites) {
        mIsLastPage = sites.size() == 0;
        if (mIsInitialQuery) {
            mIsInitialQuery = false;
            notifyEmptyListener(mIsLastPage);
        }
        for (Object site : sites) {
            add(site);
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
                notifyEmptyListener(true);
            } else {
                remove(getItemPositionById(id));
                if (mItems.size() == 0) {
                    notifyEmptyListener(true);
                }
            }
        }
    }

    public void clear() {
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

    private void notifyEmptyListener(boolean flag) {
        if (mContext instanceof EmptyListener) {
            ((EmptyListener) mContext).onEmpty(flag);
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

    private static class SiteItemViewHolder extends RecyclerView.ViewHolder {

        private ViewGroup rootView;
        private ImageView imgFav;
        private TextView textMain, textSecondary;
        private ImageButton btnMore;

        public SiteItemViewHolder(View itemView) {
            super(itemView);
            rootView = (ViewGroup) itemView.findViewById(R.id.history_item_root_view);
            imgFav = (ImageView) itemView.findViewById(R.id.history_item_img_fav);
            textMain = (TextView) itemView.findViewById(R.id.history_item_text_main);
            textSecondary = (TextView) itemView.findViewById(R.id.history_item_text_secondary);
            btnMore = (ImageButton) itemView.findViewById(R.id.history_item_btn_more);
        }
    }

    private static class DateItemViewHolder extends RecyclerView.ViewHolder {

        private TextView textDate;

        public DateItemViewHolder(View itemView) {
            super(itemView);
            textDate = (TextView) itemView.findViewById(R.id.history_item_date);
        }

    }
}
