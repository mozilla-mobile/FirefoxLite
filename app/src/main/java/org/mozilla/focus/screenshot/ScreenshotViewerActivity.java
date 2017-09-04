package org.mozilla.focus.screenshot;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.screenshot.model.ImageInfo;
import org.mozilla.focus.screenshot.model.Screenshot;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.PAN_LIMIT_INSIDE;
import static com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP;

public class ScreenshotViewerActivity extends LocaleAwareAppCompatActivity implements View.OnClickListener, QueryHandler.AsyncDeleteListener {

    public static final int REQ_CODE_NOTIFY_SCREENSHOT_DELETE = 1000;
    private static int REQUEST_CODE_VIEW_SCREENSHOT = 101;
    private static int REQUEST_CODE_EDIT_SCREENSHOT = 102;
    private static int REQUEST_CODE_SHARE_SCREENSHOT = 103;
    private static int REQUEST_CODE_DELETE_SCREENSHOT = 104;

    public static final void goScreenshotViewerActivityOnResult(Activity activity, Screenshot item) {
        Intent intent = new Intent(activity, ScreenshotViewerActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, item);
        activity.startActivityForResult(intent, REQ_CODE_NOTIFY_SCREENSHOT_DELETE);
    }

    private static final String EXTRA_SCREENSHOT = "extra_screenshot";
    private final SimpleDateFormat sSdfInfoTime = new SimpleDateFormat("MMM dd, yyyy");

    private Toolbar mBottomToolBar;
    private ImageView mImgPlaceholder;
    private SubsamplingScaleImageView mImgScreenshot;
    private Screenshot mScreenshot;
    private Uri mImageUri;
    private ArrayList<ImageInfo> mInfoItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_screenshot_viewer);

        mImgPlaceholder = (ImageView) findViewById(R.id.screenshot_viewer_image_placeholder);

        mImgScreenshot = (SubsamplingScaleImageView) findViewById(R.id.screenshot_viewer_image);
        mImgScreenshot.setPanLimit(PAN_LIMIT_INSIDE);
        mImgScreenshot.setMinimumScaleType(SCALE_TYPE_CENTER_CROP);
        mImgScreenshot.setOnClickListener(this);

        mBottomToolBar = (Toolbar) findViewById(R.id.screenshot_viewer_btm_toolbar);

        findViewById(R.id.screenshot_viewer_btn_open_url).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_edit).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_share).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_info).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_delete).setOnClickListener(this);

        mScreenshot = (Screenshot) getIntent().getSerializableExtra(EXTRA_SCREENSHOT);

        initInfoItemArray();
        if (mScreenshot != null) {
            if (new File(mScreenshot.getImageUri()).exists()) {
                if(checkPermissions()) {
                    setupView(true);
                    initScreenshotInfo(false);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_VIEW_SCREENSHOT);
                    }
                }
            } else {
                setupView(false);
                Toast.makeText(this, R.string.message_cannot_find_screenshot, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void applyLocale() {
        //  Refresh UI strings to match new Locale
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.screenshot_viewer_image:
                mBottomToolBar.setVisibility((mBottomToolBar.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
                break;
            case R.id.screenshot_viewer_btn_open_url:
                TelemetryWrapper.openCaptureLink();
                Intent urlIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mScreenshot.getUrl()));
                urlIntent.setClassName(this, MainActivity.class.getName());
                urlIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(urlIntent);
                finish();
                break;
            case R.id.screenshot_viewer_btn_edit:
                if(checkPermissions()) {
                    onEditClick();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_EDIT_SCREENSHOT);
                    }
                }
                break;
            case R.id.screenshot_viewer_btn_share:
                if(checkPermissions()) {
                    onShareClick();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_SHARE_SCREENSHOT);
                    }
                }
                break;
            case R.id.screenshot_viewer_btn_info:
                TelemetryWrapper.showCaptureInfo();
                onInfoClick();
                break;
            case R.id.screenshot_viewer_btn_delete:
                if(checkPermissions()) {
                    onDeleteClick();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_DELETE_SCREENSHOT);
                    }
                }
                break;
            default:
                break;
        }

    }


    @Override
    public void onDeleteComplete(int result, long id) {
        if (result > 0) {
            setResult(RESULT_OK, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_VIEW_SCREENSHOT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupView(true);
                initScreenshotInfo(false);
            }
        } else if(requestCode == REQUEST_CODE_EDIT_SCREENSHOT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onEditClick();
            }
        } else if(requestCode == REQUEST_CODE_SHARE_SCREENSHOT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onShareClick();
            }
        } else if(requestCode == REQUEST_CODE_DELETE_SCREENSHOT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onDeleteClick();
            }
        }
    }

    private void setupView(boolean existed) {
        mImgPlaceholder.setVisibility(existed ? View.GONE : View.VISIBLE);
        mImgScreenshot.setVisibility(existed ? View.VISIBLE : View.GONE);
        findViewById(R.id.screenshot_viewer_btn_open_url).setEnabled(existed);
        findViewById(R.id.screenshot_viewer_btn_edit).setEnabled(existed);
        findViewById(R.id.screenshot_viewer_btn_share).setEnabled(existed);
        findViewById(R.id.screenshot_viewer_btn_info).setEnabled(existed);
    }

    private void initInfoItemArray() {
        mInfoItems.clear();
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_time), ""));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_resolution), ""));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_size), ""));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_title), ""));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_url), ""));
    }

    private void initScreenshotInfo(boolean withShare) {
        if(mScreenshot != null) {
            if(mScreenshot.getTimestamp() > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(mScreenshot.getTimestamp());
                mInfoItems.get(0).data = sSdfInfoTime.format(cal.getTime());
            }
            File imgFile = new File(mScreenshot.getImageUri());
            if(imgFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mScreenshot.getImageUri(), options);
                int width = options.outWidth;
                int height = options.outHeight;
                mInfoItems.get(1).data = String.format("%d X %d", width, height);
                mInfoItems.get(2).data = getStringSizeLengthFile(imgFile.length());
            }

            mInfoItems.get(3).data = mScreenshot.getTitle();
            mInfoItems.get(4).data = mScreenshot.getUrl();

            final ImageSource imageSource;
            mImageUri = Uri.fromFile(new File(mScreenshot.getImageUri()));
            imageSource = ImageSource.uri(mImageUri);
            mImgScreenshot.setImage(imageSource, ImageViewState.ALIGN_TOP);

            if(withShare) {
                onShareClick();
            }
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void onEditClick() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentResolver cr = getContentResolver();
                Cursor ca = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID}, MediaStore.MediaColumns.DATA + "=?", new String[]{mScreenshot.getImageUri()}, null);
                if (ca != null && ca.moveToFirst()) {
                    int id = ca.getInt(ca.getColumnIndex(MediaStore.MediaColumns._ID));
                    ca.close();
                    Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    editIntent.setDataAndType(uri, "image/*");
                    editIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(editIntent, null));
                        TelemetryWrapper.editCaptureImage(true);
                    } catch (ActivityNotFoundException e) {
                        TelemetryWrapper.editCaptureImage(false);
                    }
                }
            }
        }).start();
    }

    private void onShareClick() {
        if(mImageUri != null) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, mImageUri);
            share.setType("image/*");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(share, null));
                TelemetryWrapper.shareCaptureImage(false);
            } catch (ActivityNotFoundException e) {
            }
        } else {
            setupView(true);
            initScreenshotInfo(true);
        }
    }

    private void onInfoClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.screenshot_image_viewer_dialog_title);
        builder.setAdapter(new InfoItemAdapter(this, mInfoItems), null);
        builder.setPositiveButton(R.string.action_ok, null);
        builder.create().show();
    }

    private void onDeleteClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.screenshot_image_viewer_dialog_delete_msg);
        builder.setPositiveButton(R.string.browsing_history_menu_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TelemetryWrapper.deleteCaptureImage();
                proceedDelete();
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.create().show();
    }

    public static String getStringSizeLengthFile(long size) {

        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;

        if(size < sizeMo)
            return df.format(size / sizeKb)+ " K";
        else if(size < sizeGo)
            return df.format(size / sizeMo) + " M";
        else if(size < sizeTerra)
            return df.format(size / sizeGo) + " G";

        return "";
    }

    private static class InfoItemAdapter extends ArrayAdapter<ImageInfo> {
        public InfoItemAdapter(Context context, ArrayList<ImageInfo> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageInfo item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_screenshot_info_dialog, parent, false);
            }
            TextView textTitle = (TextView) convertView.findViewById(R.id.screenshot_info_dialog_title);
            TextView textData = (TextView) convertView.findViewById(R.id.screenshot_info_dialog_data);

            textTitle.setText(item.title);
            textData.setText(item.data);
            if(position == 4) {
                convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //TODO: show context menu
                        return false;
                    }
                });
            }
            return convertView;
        }

    }

    private void proceedDelete() {
        if(mScreenshot != null) {
            File file = new File(mScreenshot.getImageUri());
            if(file.exists()) {
                try {
                    file.delete();
                } catch (Exception ex) {
                }
            }
            ScreenshotManager.getInstance().delete(mScreenshot.getId(), this);
        }
    }
}
