package org.mozilla.focus.bookmark;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.fragment.PanelFragmentStatusListener;
import org.mozilla.focus.persistence.BookmarkModel;
import org.mozilla.focus.site.SiteItemViewHolder;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.util.List;

public class BookmarkAdapter extends RecyclerView.Adapter<SiteItemViewHolder> {
    private List<BookmarkModel> bookmarkModels;
    private BookmarkPanelListener listener;

    public BookmarkAdapter(BookmarkPanelListener listener) {
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
        final BookmarkModel item = getItem(position);
        if (item == null) {
            return;
        }

        holder.rootView.setTag(item.getId());
        holder.textMain.setText(item.getTitle());
        holder.textSecondary.setText(item.getUrl());
        holder.rootView.setOnClickListener(v -> {
            listener.onItemClicked(item.getUrl());
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
        return (bookmarkModels != null ? bookmarkModels.size() : 0);
    }

    public void setData(List<BookmarkModel> bookmarkModels) {
        this.bookmarkModels = bookmarkModels;
        if (getItemCount() == 0) {
            listener.onStatus(PanelFragment.VIEW_TYPE_EMPTY);
        } else {
            listener.onStatus(PanelFragment.VIEW_TYPE_NON_EMPTY);
        }
        notifyDataSetChanged();
    }

    private BookmarkModel getItem(int index) {
        if (index >= 0 && bookmarkModels != null && bookmarkModels.size() > index) {
            return bookmarkModels.get(index);
        } else {
            return null;
        }
    }

    public interface BookmarkPanelListener extends PanelFragmentStatusListener {
        void onItemClicked(String url);

        void onItemDeleted(BookmarkModel bookmark);

        void onItemEdited(BookmarkModel bookmark);
    }
}
