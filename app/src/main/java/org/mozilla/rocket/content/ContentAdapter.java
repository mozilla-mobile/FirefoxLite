package org.mozilla.rocket.content;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.fragment.PanelFragmentStatusListener;

import org.mozilla.focus.site.SiteItemViewHolder;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.rocket.bhaskar.ItemPojo;

import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<SiteItemViewHolder> {
    private List<ItemPojo> items;
    private ContentPanelListener listener;

    public ContentAdapter(ContentPanelListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SiteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_website, parent, false);
        return new SiteItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteItemViewHolder holder, int position) {
        final ItemPojo item = getItem(position);
        if (item == null) {
            return;
        }
        String favIconUri = item.coverPic.substring(2,item.coverPic.length()-2);
        if (favIconUri != null) {
            Glide.with(holder.imgFav.getContext())
                    .asBitmap()
                    .load(favIconUri)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            if (DimenUtils.iconTooBlurry(holder.imgFav.getResources(), resource.getWidth())) {
                                setImageViewWithDefaultBitmap(holder.imgFav, item.detailUrl);
                            } else {
                                holder.imgFav.setImageBitmap(resource);
                            }
                        }
                                });
                                } else {
                                setImageViewWithDefaultBitmap(holder.imgFav, item.detailUrl);
                                }
                                holder.rootView.setTag(item.category);
                                holder.textMain.setText(item.title);
                                holder.textSecondary.setText(item.description);
                                holder.rootView.setOnClickListener(v -> {
                                listener.onItemClicked(item.detailUrl);
                                });
final PopupMenu popupMenu = new PopupMenu(holder.btnMore.getContext(), holder.btnMore);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
        if (menuItem.getItemId() == R.id.remove) {
        listener.onItemDeleted(item);
        }
        if (menuItem.getItemId() == R.id.edit) {
        listener.onItemEdited(item);
        }
        return false;
        });
        popupMenu.inflate(R.menu.menu_bookmarks);
        holder.btnMore.setOnClickListener(v -> {
        popupMenu.show();
        TelemetryWrapper.showBookmarkContextMenu();
        });
        }

@Override
public int getItemCount() {
        return (items != null ? items.size() : 0);
        }

public void setData(List<ItemPojo> list) {
        this.items = list;
        if (getItemCount() == 0) {
        listener.onStatus(PanelFragment.VIEW_TYPE_EMPTY);
        } else {
        listener.onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY);
        }
        notifyDataSetChanged();
        }

private ItemPojo getItem(int index) {
        if (index >= 0 && items != null && items.size() > index) {
        return items.get(index);
        } else {
        return null;
        }
        }

public interface ContentPanelListener extends PanelFragmentStatusListener {
    void onItemClicked(String url);

    void onItemDeleted(ItemPojo item);

    void onItemEdited(ItemPojo item);
}

    private void setImageViewWithDefaultBitmap(ImageView imageView, String url) {
        imageView.setImageBitmap(DimenUtils.getInitialBitmap(imageView.getResources(), FavIconUtils.getRepresentativeCharacter(url), Color.WHITE));
    }
}