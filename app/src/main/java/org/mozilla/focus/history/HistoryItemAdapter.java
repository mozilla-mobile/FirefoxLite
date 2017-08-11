package org.mozilla.focus.history;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;

/**
 * Created by joseph on 08/08/2017.
 */

public class HistoryItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener,
        BrowsingHistoryManager.AsyncQueryListener, BrowsingHistoryManager.AsyncDeleteListener {

    private static final int VIEW_TYPE_SITE = 1;
    private static final int VIEW_TYPE_DATE = 2;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private Context mContext;

    public interface EmptyListener {
        void onEmpty(boolean flag);
    }
    public HistoryItemAdapter(RecyclerView recyclerView, Context context) {
        mRecyclerView = recyclerView;
        mContext = context;
        BrowsingHistoryManager.getInstance().query(this);
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
            final Site item = getItem(position);

            if(item != null) {
                final SiteItemViewHolder siteVH = (SiteItemViewHolder) holder;
                siteVH.rootView.setOnClickListener(this);
                siteVH.textMain.setText(item.getTitle());
                siteVH.textSecondary.setText(item.getUrl());
                siteVH.imgFav.setImageBitmap(item.getFavIcon());

                Context wrapper = new ContextThemeWrapper(mContext, R.style.Theme_AppCompat_Light);
                final PopupMenu popupMenu = new PopupMenu(wrapper, siteVH.btnMore);
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
            final DateItemViewHolder dateVH = (DateItemViewHolder) holder;
            dateVH.textDate.setText(R.string.browsing_history_date_today);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_DATE : VIEW_TYPE_SITE;
    }

    @Override
    public int getItemCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public void onClick(View v) {
        int position = mRecyclerView.getChildLayoutPosition(v);
        //TODO handle site item click
        if(position == 0) {

        }
    }

    @Override
    public void onQueryComplete(Cursor cursor) {
        if (mContext instanceof EmptyListener) {
            ((EmptyListener) mContext).onEmpty(cursor == null || cursor.getCount() == 0);
        }
        changeCursor(cursor);
    }

    @Override
    public void onDeleteComplete(int result) {
        if (result > 0) {
            BrowsingHistoryManager.getInstance().query(this);
        }
    }

    public void clear() {
        BrowsingHistoryManager.getInstance().deleteAll(this);
    }

    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    private Cursor swapCursor(Cursor newCursor) {
        if (mCursor == newCursor) {
            return null;
        }

        Cursor oldCursor = mCursor;
        mCursor = newCursor;
        if (newCursor != null) {
            notifyDataSetChanged();
        }
        return oldCursor;
    }

    private Site getItem(int position) {
        if (position > 0 && mCursor != null) {
            mCursor.moveToPosition(position - 1);
            return BrowsingHistoryManager.cursorToSite(mCursor);
        } else {
            return null;
        }
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
