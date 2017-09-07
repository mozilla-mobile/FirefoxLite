package org.mozilla.focus.widget;

import android.app.DownloadManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import org.mozilla.focus.R;
import org.mozilla.focus.download.DownloadInfo;
import org.mozilla.focus.download.DownloadInfoManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.IntentUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by anlin on 01/08/2017.
 */

public class DownloadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements DownloadInfoManager.AsyncQueryListener{

    private static final List<String> SPECIFIC_FILE_EXTENSION
            = Arrays.asList("apk","zip","gz","tar","7z","rar","war");
    private List<DownloadInfo> mDownloadInfo = new ArrayList<>();
    private static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_NON_EMPTY = 1;
    private Context mContext;
    private int mItemCount = 0;

    public DownloadListAdapter(Context context){
        mContext = context;
        loadMore();
    }

    public void loadMore(){
        DownloadInfoManager.getInstance().query(mItemCount,50,this);
    }

    public void updateItem(DownloadInfo downloadInfo){

        for (int i = 0;i<mDownloadInfo.size();i++){
            if (mDownloadInfo.get(i).getDownloadId().equals(downloadInfo.getDownloadId())){
                mDownloadInfo.remove(i);
                mDownloadInfo.add(downloadInfo);

                this.notifyDataSetChanged();
                break;
            }
        }
    }

    private void remove(int position){
        long downloadId = mDownloadInfo.get(position).getDownloadId();
        DownloadInfoManager.getInstance().delete(downloadId,null);

        mDownloadInfo.remove(position);

        this.notifyDataSetChanged();
    }

    private void delete(int position){
        try {
            new File(URI.create(mDownloadInfo.get(position).getFileUri()).getPath()).delete();

        }catch (Exception e){
            Log.v(this.getClass().getSimpleName(),""+e.getMessage());
        }

        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.remove(mDownloadInfo.get(position).getDownloadId());
        remove(position);
    }

    private void cancel(int position){
        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.remove(mDownloadInfo.get(position).getDownloadId());

        remove(position);
    }

    @Override
    public int getItemViewType(int position) {
        return mDownloadInfo.isEmpty() ? VIEW_TYPE_EMPTY : VIEW_TYPE_NON_EMPTY;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;
        if (VIEW_TYPE_NON_EMPTY == viewType){
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_menu_cell,parent,false);
            return new DownloadViewHolder(itemView);
        }else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_empty,parent,false);
            return new DownloadEmptyViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof DownloadViewHolder){
            DownloadViewHolder holder = (DownloadViewHolder) viewHolder;
            DownloadInfo downloadInfo = mDownloadInfo.get(position);

            holder.title.setText(downloadInfo.getFileName());
            holder.icon.setImageResource(mappingIcon(downloadInfo));

            String subtitle="";
            if (DownloadManager.STATUS_SUCCESSFUL == downloadInfo.getStatus()) {
                subtitle = downloadInfo.getSize() + ", " + downloadInfo.getDate();
            } else {
                subtitle = statusConvertStr(downloadInfo.getStatus());
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
                                    TelemetryWrapper.downloadRemoveFile();
                                    popupMenu.dismiss();
                                    return true;
                                case R.id.delete:
                                    delete(position);
                                    TelemetryWrapper.downloadDeleteFile();
                                    popupMenu.dismiss();
                                    return true;
                                case R.id.cancel:
                                    cancel(position);
                                    popupMenu.dismiss();
                                    return true;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });

                    if (DownloadManager.STATUS_RUNNING == mDownloadInfo.get(position).getStatus()){

                        popupMenu.getMenu().findItem(R.id.remove).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.delete).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.cancel).setVisible(true);

                    }else {
                        popupMenu.getMenu().findItem(R.id.remove).setVisible(true);
                        popupMenu.getMenu().findItem(R.id.delete).setVisible(true);
                        popupMenu.getMenu().findItem(R.id.cancel).setVisible(false);
                    }
                    popupMenu.show();
                    TelemetryWrapper.showFileContextMenu();
                }
            });

            holder.itemView.setTag(downloadInfo);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DownloadInfo download = (DownloadInfo) view.getTag();
                    IntentUtils.intentOpenFile(view.getContext(),download.getMediaUri(),download.getMimeType());
                    TelemetryWrapper.downloadOpenFile(false);
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        if (!mDownloadInfo.isEmpty()){
            return mDownloadInfo.size();
        }else {
            return 1;
        }
    }

    @Override
    public void onQueryComplete(List downloadInfoList) {
        mDownloadInfo.addAll(downloadInfoList);
        mItemCount = mDownloadInfo.size();
        this.notifyDataSetChanged();
    }

    private String statusConvertStr(int status){
        switch(status) {
            case DownloadManager.STATUS_PAUSED:
                return mContext.getResources().getString(R.string.pause);
            case DownloadManager.STATUS_PENDING:
                return mContext.getResources().getString(R.string.pending);
            case DownloadManager.STATUS_RUNNING:
                return mContext.getResources().getString(R.string.running);
            case DownloadManager.STATUS_SUCCESSFUL:
                return mContext.getResources().getString(R.string.successful);
            case DownloadManager.STATUS_FAILED:
                return mContext.getResources().getString(R.string.failed);
            default:
                return mContext.getResources().getString(R.string.unknown);
        }
    }

    public int mappingIcon(DownloadInfo downloadInfo){

        if (SPECIFIC_FILE_EXTENSION.contains(downloadInfo.getFileExtension())){
           return "apk".equals(downloadInfo.getFileExtension()) ? R.drawable.file_app : R.drawable.file_compressed;
        }else {

            if (!TextUtils.isEmpty(downloadInfo.getMimeType())){
                String mimeType = downloadInfo.getMimeType().substring(0,downloadInfo.getMimeType().indexOf("/"));
                switch (mimeType){
                    case "text":
                        return R.drawable.file_document;
                    case "image":
                        return R.drawable.file_image;
                    case "audio":
                        return R.drawable.file_music;
                    case "video":
                        return R.drawable.file_video;
                    default:
                        return R.drawable.file_document;
                }
            }else {
                return R.drawable.file_document;
            }
        }
    }

    public class DownloadViewHolder extends RecyclerView.ViewHolder{

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        ImageView icon;
        TextView title;
        TextView subtitle;
        FrameLayout action;

        public DownloadViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.img);
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            action = (FrameLayout) itemView.findViewById(R.id.menu_action);

        }
    }

    public class DownloadEmptyViewHolder extends RecyclerView.ViewHolder{

        @SuppressFBWarnings("URF_UNREAD_FIELD")
        ImageView imag;

        public DownloadEmptyViewHolder(View itemView) {
            super(itemView);
            imag = (ImageView) itemView.findViewById(R.id.img);
        }
    }
}
