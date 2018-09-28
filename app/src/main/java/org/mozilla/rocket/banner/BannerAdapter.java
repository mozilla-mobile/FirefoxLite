package org.mozilla.rocket.banner;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.rocket.util.LoggerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BannerAdapter extends RecyclerView.Adapter<BannerViewHolder> {

    private static final String LOG_TAG = "BannerAdapter";

    @IntDef({UNKNOWN_VIEW_TYPE, BasicViewHolder.VIEW_TYPE, SingleButtonViewHolder.VIEW_TYPE, FourSitesViewHolder.VIEW_TYPE})
    @interface ViewType {}

    private Context context;
    private List<BannerDAO> DAOs;
    private OnClickListener onClickListener;
    private static final int UNKNOWN_VIEW_TYPE = -1;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.context = recyclerView.getContext();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.context = null;
    }

    public BannerAdapter(@NonNull String[] configs, OnClickListener onClickListener) throws JSONException {
        this.DAOs = new ArrayList<>();
        this.onClickListener = onClickListener;
        for (int i = 0 ; i < configs.length ; i++) {
            String config = configs[i];
            JSONObject jsonObject = new JSONObject(config);
            BannerDAO thisDAO = new BannerDAO();
            thisDAO.type = jsonObject.getString(BannerDAO.TYPE_KEY);
            thisDAO.values = jsonObject.getJSONArray(BannerDAO.VALUES_KEY);
            thisDAO.id = jsonObject.getString(BannerDAO.ID_KEY);
            // We always write full item into cache, but discard a page if it is not known in this
            // apk version. With this once the user upgrades, the cache is still usable.
            // We still inflate an item with SingleButtonViewHolder if it for unknown reason managed
            // to pass this check. (See: onCreateViewHolder)
            if (getItemViewType(thisDAO.type) == UNKNOWN_VIEW_TYPE) {
                LoggerWrapper.throwOrWarn(LOG_TAG, String.format(Locale.US, "Unknown view type: %s in page %d", thisDAO.type, i));
                continue;
            }
            this.DAOs.add(thisDAO);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType(DAOs.get(position).type);
    }

    private int getItemViewType(String type) {
        switch (type) {
            case SingleButtonViewHolder.VIEW_TYPE_NAME:
                return SingleButtonViewHolder.VIEW_TYPE;
            case FourSitesViewHolder.VIEW_TYPE_NAME:
                return FourSitesViewHolder.VIEW_TYPE;
            case BasicViewHolder.VIEW_TYPE_NAME:
                return BasicViewHolder.VIEW_TYPE;
            default:
                return UNKNOWN_VIEW_TYPE;
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
            case UNKNOWN_VIEW_TYPE:
            default:
                return new BasicViewHolder(parent, onClickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.onBindViewHolder(context, DAOs.get(position));
    }

    @Override
    public int getItemCount() {
        return DAOs.size();
    }
}
