package org.mozilla.focus.screenshot;

import android.app.Activity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.glide.GlideApp;
import org.mozilla.focus.history.model.DateSection;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.screenshot.model.Screenshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by joseph on 17/08/2017.
 */

public class ScreenshotItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener,
        QueryHandler.AsyncQueryListener, QueryHandler.AsyncDeleteListener {

    public static final int VIEW_TYPE_SCREENSHOT = 1;
    private static final int VIEW_TYPE_DATE = 2;

    private static final int PAGE_SIZE = 20;

    private List mItems = new ArrayList();
    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private Activity mActivity;
    private EmptyListener mEmptyListener;
    private boolean mIsInitialQuery;
    private boolean mIsLoading;
    private boolean mIsLastPage;
    private int mCurrentCount;

    public interface EmptyListener {
        void onEmpty(boolean flag);
    }

    public ScreenshotItemAdapter(RecyclerView recyclerView, Activity activity, EmptyListener emptyListener, GridLayoutManager layoutManager) {
        mRecyclerView = recyclerView;
        mActivity = activity;
        mEmptyListener = emptyListener;
        mLayoutManager = layoutManager;
        mLayoutManager.setSpanSizeLookup(mSpanSizeHelper);
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
        if (viewType == VIEW_TYPE_SCREENSHOT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_screenshot_grid_cell, parent, false);
            return new GirdItemViewHolder(v);
        } else if (viewType == VIEW_TYPE_DATE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_screenshot_date, parent, false);
            return new DateItemViewHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof GirdItemViewHolder) {
            final GirdItemViewHolder gridVH = (GirdItemViewHolder) holder;
            gridVH.rootView.setOnClickListener(this);
            final Screenshot item = (Screenshot) mItems.get(position);
            GlideApp
                    .with(mActivity)
                    .asBitmap()
                    .load(item.getImageUri())
                    .into(gridVH.img);
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
            return VIEW_TYPE_SCREENSHOT;
        }
    }

    public int getAdjustPosition(int position) {
        int adjustPosition = position;
        if (position < mItems.size()) {
            int lastDateSectionIndex = 0;
            for (int i = 0; i <= position; i++) {
                if (mItems.get(i) instanceof DateSection)
                    lastDateSectionIndex = i;
            }
            if (lastDateSectionIndex == 0)
                adjustPosition = position - 1;
            else
                adjustPosition = position - 1 - lastDateSectionIndex;
        }
        return adjustPosition;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onClick(View v) {
        final int position = mRecyclerView.getChildLayoutPosition(v);
        if (position != RecyclerView.NO_POSITION) {
            Object item = mItems.get(position);
            if (item instanceof Screenshot) {
                ScreenshotViewerActivity.goScreenshotViewerActivityOnResult(mActivity, (Screenshot) item);
            }
        }
    }

    @Override
    public void onQueryComplete(List result) {
        mIsLastPage = result.size() == 0;
        if (mIsInitialQuery) {
            mIsInitialQuery = false;
            notifyEmptyListener(mIsLastPage);
        }
        for (Object item : result) {
            add(item);
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

    private void add(Object item) {
        if (mItems.size() > 0 && isSameDay(((Screenshot) mItems.get(mItems.size() - 1)).getTimestamp(), ((Screenshot) item).getTimestamp())) {
            mItems.add(item);
            notifyItemInserted(mItems.size());
        } else {
            mItems.add(new DateSection(((Screenshot) item).getTimestamp()));
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
        if (previous instanceof Screenshot || next instanceof Screenshot) {
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
        ScreenshotManager.getInstance().query(mCurrentCount, PAGE_SIZE - (mCurrentCount % PAGE_SIZE), this);
    }

    private void notifyEmptyListener(boolean flag) {
        if (mEmptyListener != null) {
            mEmptyListener.onEmpty(flag);
        }
    }

    private int getItemPositionById(long id) {
        for (int i = 0; i < mItems.size(); i++) {
            Object item = mItems.get(i);
            if (item instanceof Screenshot) {
                if (id == ((Screenshot) item).getId()) {
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

    private static class GirdItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView img;
        private View rootView;

        public GirdItemViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.screenshot_grid_img_cell);
            rootView = itemView.findViewById(R.id.screenshot_grid_rootview);
        }
    }

    private static class DateItemViewHolder extends RecyclerView.ViewHolder {

        private TextView textDate;

        public DateItemViewHolder(View itemView) {
            super(itemView);
            textDate = (TextView) itemView.findViewById(R.id.screenshot_item_date);
        }

    }

    GridLayoutManager.SpanSizeLookup mSpanSizeHelper = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            switch (getItemViewType(position)) {
                case VIEW_TYPE_SCREENSHOT:
                    return 1;
                case VIEW_TYPE_DATE:
                    return 3;
                default:
                    return 1;
            }
        }
    };
}