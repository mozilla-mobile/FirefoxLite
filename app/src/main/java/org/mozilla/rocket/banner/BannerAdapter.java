package org.mozilla.rocket.banner;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerAdapter extends RecyclerView.Adapter<BannerViewHolder> {

    @IntDef({BasicViewHolder.VIEW_TYPE, SingleButtonViewHolder.VIEW_TYPE, FourSitesViewHolder.VIEW_TYPE})
    @interface ViewType {}

    private Context context;
    private BannerDAO[] DAOs;
    private OnClickListener onClickListener;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.context = recyclerView.getContext();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.context = null;
    }

    public BannerAdapter(@NonNull String[] configs, OnClickListener onClickListener) throws JSONException {
        this.DAOs = new BannerDAO[configs.length];
        this.onClickListener = onClickListener;
        for (int i = 0; i < configs.length ; i++) {
            JSONObject jsonObject = new JSONObject(configs[i]);
            BannerDAO thisDAO = new BannerDAO();
            thisDAO.type = jsonObject.getString(BannerDAO.TYPE_KEY);
            thisDAO.values = jsonObject.getJSONArray(BannerDAO.VALUES_KEY);
            thisDAO.id = jsonObject.getString(BannerDAO.ID_KEY);
            this.DAOs[i] = thisDAO;
        }
    }

    @Override
    public int getItemViewType(int position) {
        switch (DAOs[position].type) {
            case SingleButtonViewHolder.VIEW_TYPE_NAME:
                return SingleButtonViewHolder.VIEW_TYPE;
            case FourSitesViewHolder.VIEW_TYPE_NAME:
                return FourSitesViewHolder.VIEW_TYPE;
            case BasicViewHolder.VIEW_TYPE_NAME:
            default:
                return BasicViewHolder.VIEW_TYPE;
        }
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, @ViewType int viewType) {
        switch (viewType) {
            case SingleButtonViewHolder.VIEW_TYPE:
                return new SingleButtonViewHolder(parent, onClickListener);
            case FourSitesViewHolder.VIEW_TYPE:
                return new FourSitesViewHolder(parent, onClickListener);
            case BasicViewHolder.VIEW_TYPE:
            default:
                return new BasicViewHolder(parent, onClickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.onBindViewHolder(context, DAOs[position]);
    }

    @Override
    public int getItemCount() {
        return DAOs.length;
    }
}
