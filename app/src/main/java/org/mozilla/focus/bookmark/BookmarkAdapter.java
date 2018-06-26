package org.mozilla.focus.bookmark;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.EditBookmarkActivity;
import org.mozilla.focus.activity.EditBookmarkActivityKt;
import org.mozilla.focus.fragment.ItemClosingPanelFragmentStatusListener;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.site.SiteItemViewHolder;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.rocket.temp.TempInMemoryBookmarkRepository;

import java.util.UUID;

public class BookmarkAdapter extends RecyclerView.Adapter<SiteItemViewHolder> {
    private ItemClosingPanelFragmentStatusListener listener;
    private Context context;

    public BookmarkAdapter(Context context, ItemClosingPanelFragmentStatusListener listener) {
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public SiteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_website, parent, false);
        return new SiteItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteItemViewHolder holder, int position) {
        TempInMemoryBookmarkRepository.Bookmark item = TempInMemoryBookmarkRepository.getInstance().list().get(position);
        holder.rootView.setTag(item.getUuid());
        holder.textMain.setText(item.getName());
        holder.textSecondary.setText(item.getAddress());
        holder.rootView.setOnClickListener(v -> {
            ScreenNavigator.get(context).showBrowserScreen(item.getAddress(), true, false);
            listener.onItemClicked();
            TelemetryWrapper.bookmarkOpenItem();
        });
        final PopupMenu popupMenu = new PopupMenu(context, holder.btnMore);
        popupMenu.setOnMenuItemClickListener( menuItem -> {
            if (menuItem.getItemId() == R.id.remove) {
                TempInMemoryBookmarkRepository.getInstance().removeSingle((UUID) holder.rootView.getTag());
                notifyDataSetChanged();
                TelemetryWrapper.bookmarkRemoveItem();
            }
            if (menuItem.getItemId() == R.id.edit) {
                context.startActivity(new Intent(context, EditBookmarkActivity.class).putExtra(EditBookmarkActivityKt.ITEM_UUID_KEY, item.getUuid()));
                TelemetryWrapper.bookmarkEditItem();
            }
            return false;
        });
        popupMenu.inflate(R.menu.menu_bookmarks);
        holder.btnMore.setOnClickListener( v -> {
            popupMenu.show();
            TelemetryWrapper.showBookmarkContextMenu();
        });
    }

    @Override
    public int getItemCount() {
        int count = TempInMemoryBookmarkRepository.getInstance().list().size();
        if (count == 0) {
            listener.onStatus(PanelFragment.VIEW_TYPE_EMPTY);
        } else {
            listener.onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY);
        }
        return count;
    }
}
