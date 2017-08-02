package org.mozilla.focus.widget;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;
import org.mozilla.focus.R;
import org.mozilla.focus.greenDAO.DBUtils;
import org.mozilla.focus.greenDAO.DownloadInfo;
import org.mozilla.focus.greenDAO.DownloadInfoEntity;
import org.mozilla.focus.greenDAO.DownloadInfoEntityDao;

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
        List<DownloadInfoEntity> downloadIdList = DBUtils.getDbService().getDao().loadAll();
        mDownloadInfo.clear();

        for (int i = 0 ;i<downloadIdList.size();i++){
            DownloadInfoEntity entity = downloadIdList.get(i);
            DownloadInfo downloadInfo = new DownloadInfo(entity.getDownLoadId(),entity.getFileName());
            mDownloadInfo.add(downloadInfo);
        }

        this.notifyDataSetChanged();
    }

    public void updateItem(long downloadId){

        QueryBuilder<DownloadInfoEntity> queryBuilder = DBUtils.getDbService().getDao().queryBuilder();
        Property downloadIdProperty = DownloadInfoEntityDao.Properties.DownLoadId;
        DownloadInfoEntity entity = queryBuilder.where(downloadIdProperty.eq(downloadId)).unique();
        DownloadInfo downloadInfo = new DownloadInfo(entity.getDownLoadId(),entity.getFileName());

        for (int i = 0;i<mDownloadInfo.size();i++){
            if (mDownloadInfo.get(i).getDownloadId().equals(entity.getDownLoadId())){
                mDownloadInfo.set(i,downloadInfo);
                this.notifyDataSetChanged();
                break;
            }
        }
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
        @SuppressFBWarnings("URF_UNREAD_FIELD")
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
