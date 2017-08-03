package org.mozilla.focus.widget;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import org.mozilla.focus.R;
import org.mozilla.focus.greenDAO.DBUtils;
import org.mozilla.focus.greenDAO.DownloadInfo;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadListAdapter extends RecyclerView.Adapter<DownloadListAdapter.DownloadViewHolder> {

    private List<DownloadInfo> mDownloadInfo = new ArrayList<>();

    public DownloadListAdapter(){
        fetchEntity();
    }

    public void fetchEntity(){
        mDownloadInfo.clear();
        mDownloadInfo = DBUtils.getDbService().getAllDownloadInfo();
        this.notifyDataSetChanged();
    }

    public void updateItem(long downloadId){

        for (int i = 0;i<mDownloadInfo.size();i++){
            if (mDownloadInfo.get(i).getDownloadId().equals(downloadId)){
                DownloadInfo downloadInfo
                        = new DownloadInfo(downloadId,mDownloadInfo.get(i).getFileName());
                mDownloadInfo.set(i,downloadInfo);
                this.notifyDataSetChanged();
                break;
            }
        }
    }

    private void remove(int position){
        long downloadId = mDownloadInfo.get(position).getDownloadId();
        DBUtils.getDbService().delete(downloadId);

        mDownloadInfo.remove(position);

        this.notifyDataSetChanged();
    }

    private void delete(int position){
        try {
            new File(URI.create(mDownloadInfo.get(position).getUri()).getPath()).delete();

        }catch (Exception e){
            Log.v(this.getClass().getSimpleName(),""+e.getMessage());
        }
        remove(position);
    }

    @Override
    public DownloadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_menu_cell,parent,false);
        return new DownloadViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DownloadViewHolder holder, int position) {
        DownloadInfo downloadInfo = mDownloadInfo.get(position);

        holder.title.setText(downloadInfo.getFileName());

        String subtitle="";
        if ("successful".equalsIgnoreCase(downloadInfo.getStatus())) {
            subtitle = downloadInfo.getSize() + "," + downloadInfo.getDate();
        } else {
            subtitle = downloadInfo.getStatus();
        }

        holder.subtitle.setText(subtitle);

        holder.action.setTag(position);
        holder.action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int position = (int) view.getTag();
                final PopupMenu popupMenu = new PopupMenu(view.getContext(),view);
                popupMenu.getMenuInflater().inflate(R.menu.menu_delete,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        switch (menuItem.getItemId()){
                            case R.id.remove:
                                remove(position);
                                break;
                            case R.id.delete:
                                delete(position);
                                break;
                            default:
                                break;
                        }
                        popupMenu.dismiss();
                        return false;
                    }
                });

                popupMenu.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDownloadInfo.size();
    }

    public class DownloadViewHolder extends RecyclerView.ViewHolder{

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        ImageView icon;
        TextView title;
        TextView subtitle;
        ImageView action;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.img);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            action = (ImageView) itemView.findViewById(R.id.menu_action);

        }
    }
}
