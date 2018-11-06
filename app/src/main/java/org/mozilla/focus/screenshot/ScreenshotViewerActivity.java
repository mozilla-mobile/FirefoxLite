/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.BaseActivity;
import org.mozilla.focus.permission.PermissionHandle;
import org.mozilla.focus.permission.PermissionHandler;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.screenshot.model.ImageInfo;
import org.mozilla.focus.screenshot.model.Screenshot;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.rocket.content.ContentPortalViewState;
import org.mozilla.threadutils.ThreadUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.PAN_LIMIT_INSIDE;

public class ScreenshotViewerActivity extends BaseActivity implements View.OnClickListener, QueryHandler.AsyncDeleteListener {

    public static final String EXTRA_SCREENSHOT_ITEM_ID = "extra_screenshot_item_id";
    public static final String EXTRA_URL = "extra_url";
    public static final int REQ_CODE_VIEW_SCREENSHOT = 1000;
    public static final int RESULT_NOTIFY_SCREENSHOT_IS_DELETED = 100;
    public static final int RESULT_OPEN_URL = RESULT_NOTIFY_SCREENSHOT_IS_DELETED + 1;

    private PermissionHandler permissionHandler;
    private static final int ACTION_VIEW = 0;
    private static final int ACTION_EDIT = ACTION_VIEW + 1;
    private static final int ACTION_SHARE = ACTION_VIEW + 2;
    private static final int ACTION_DELETE = ACTION_VIEW + 3;

    private static final int REQUEST_CODE_VIEW_SCREENSHOT = 101;
    private static final int REQUEST_CODE_EDIT_SCREENSHOT = 102;
    private static final int REQUEST_CODE_SHARE_SCREENSHOT = 103;
    private static final int REQUEST_CODE_DELETE_SCREENSHOT = 104;
    private static final long DELAY_MILLIS_TO_SHOW_PROGRESS_BAR = 700;

    public static final void goScreenshotViewerActivityOnResult(Activity activity, Screenshot item) {
        Intent intent = new Intent(activity, ScreenshotViewerActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, item);
        activity.startActivityForResult(intent, REQ_CODE_VIEW_SCREENSHOT);
    }

    private static final String EXTRA_SCREENSHOT = "extra_screenshot";

    private Toolbar mBottomToolBar;
    private ImageView mImgPlaceholder;
    private SubsamplingScaleImageView mImgScreenshot;
    private Screenshot mScreenshot;
    private ProgressBar mProgressBar;
    private Uri mImageUri;
    private ArrayList<ImageInfo> mInfoItems = new ArrayList<>();
    private boolean mIsImageReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionHandler = new PermissionHandler(new PermissionHandle() {

            private void viewScreenshot() {
                setupView(true);
                initScreenshotInfo(false);
            }

            private void doAction(int actionId) {
                switch (actionId) {
                    case ACTION_VIEW:
                        viewScreenshot();
                        break;
                    case ACTION_EDIT:
                        onEditClick();
                        break;
                    case ACTION_SHARE:
                        onShareClick();
                        break;
                    case ACTION_DELETE:
                        onDeleteClick();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public void doActionDirect(String permission, int actionId, Parcelable params) {
                doAction(actionId);
            }

            @Override
            public void doActionGranted(String permission, int actionId, Parcelable params) {
                doAction(actionId);
            }

            @Override
            public void doActionSetting(String permission, int actionId, Parcelable params) {
                doAction(actionId);
            }

            @Override
            public void doActionNoPermission(String permission, int actionId, Parcelable params) {
                // Do nothing
            }

            @Override
            public Snackbar makeAskAgainSnackBar(int actionId) {
                return PermissionHandler.makeAskAgainSnackBar(ScreenshotViewerActivity.this, findViewById(R.id.root), R.string.permission_toast_storage);
            }

            @Override
            public void requestPermissions(int actionId) {
                ActivityCompat.requestPermissions(ScreenshotViewerActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, actionId);
            }

            @Override
            public void permissionDeniedToast(int actionId) {
                Toast.makeText(ScreenshotViewerActivity.this, R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show();
            }
        });

        setContentView(R.layout.activity_screenshot_viewer);

        mImgPlaceholder = (ImageView) findViewById(R.id.screenshot_viewer_image_placeholder);

        mImgScreenshot = (SubsamplingScaleImageView) findViewById(R.id.screenshot_viewer_image);
        mImgScreenshot.setPanLimit(PAN_LIMIT_INSIDE);
        mImgScreenshot.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM);
        mImgScreenshot.setMinScale(1);
        mImgScreenshot.setOnClickListener(this);
        mImgScreenshot.setOnImageEventListener(onImageEventListener);

        mProgressBar = (ProgressBar) findViewById(R.id.screenshot_progressbar);

        mBottomToolBar = (Toolbar) findViewById(R.id.screenshot_viewer_btm_toolbar);

        findViewById(R.id.screenshot_viewer_btn_open_url).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_edit).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_share).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_info).setOnClickListener(this);
        findViewById(R.id.screenshot_viewer_btn_delete).setOnClickListener(this);

        mScreenshot = (Screenshot) getIntent().getSerializableExtra(EXTRA_SCREENSHOT);

        initInfoItemArray();
        if (mScreenshot != null) {
            permissionHandler.tryAction(this, Manifest.permission.READ_EXTERNAL_STORAGE, ACTION_VIEW, null);
        } else {
            finish();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        permissionHandler.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        permissionHandler.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHandler.onActivityResult(this, requestCode, resultCode, data);
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
                TelemetryWrapper.openCaptureLink(mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                ContentPortalViewState.setLastSessionContent(false);
                Intent urlIntent = new Intent();
                urlIntent.putExtra(EXTRA_URL, mScreenshot.getUrl());
                setResult(RESULT_OPEN_URL, urlIntent);
                finish();
                break;
            case R.id.screenshot_viewer_btn_edit:
                permissionHandler.tryAction(this, Manifest.permission.READ_EXTERNAL_STORAGE, ACTION_EDIT, null);
                break;
            case R.id.screenshot_viewer_btn_share:
                permissionHandler.tryAction(this, Manifest.permission.READ_EXTERNAL_STORAGE, ACTION_SHARE, null);
                break;
            case R.id.screenshot_viewer_btn_info:
                TelemetryWrapper.showCaptureInfo(mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                onInfoClick();
                break;
            case R.id.screenshot_viewer_btn_delete:
                permissionHandler.tryAction(this, Manifest.permission.READ_EXTERNAL_STORAGE, ACTION_DELETE, null);
                break;
            default:
                break;
        }

    }


    @Override
    public void onDeleteComplete(int result, long id) {
        if (result > 0) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SCREENSHOT_ITEM_ID, id);
            setResult(RESULT_NOTIFY_SCREENSHOT_IS_DELETED, intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
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
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_time1, "")));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_resolution1, "")));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_size1, "")));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_title1, "")));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_category, "")));
        mInfoItems.add(new ImageInfo(getString(R.string.screenshot_image_viewer_dialog_info_url1, "")));
    }

    private void initScreenshotInfo(boolean withShare) {
        if (mScreenshot != null) {
            new ScreenshotInfoTask(this, mScreenshot, mInfoItems, withShare).execute();
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void onEditClick() {
        ThreadUtils.postToBackgroundThread(new Runnable() {
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
                        TelemetryWrapper.editCaptureImage(true, mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                    } catch (ActivityNotFoundException e) {
                        TelemetryWrapper.editCaptureImage(false, mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                    }
                }
            }
        });
    }

    private void onShareClick() {
        if (mImgScreenshot.isImageLoaded()) {
            ThreadUtils.postToBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    ContentResolver cr = getContentResolver();
                    Cursor ca = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.MediaColumns._ID}, MediaStore.MediaColumns.DATA + "=?", new String[]{mScreenshot.getImageUri()}, null);
                    if (ca != null && ca.moveToFirst()) {
                        int id = ca.getInt(ca.getColumnIndex(MediaStore.MediaColumns._ID));
                        ca.close();
                        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        share.setType("image/*");
                        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(Intent.createChooser(share, null));
                            TelemetryWrapper.shareCaptureImage(false, mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                        } catch (ActivityNotFoundException e) {
                        }
                    }
                }
            });
        } else {
            setupView(true);
            initScreenshotInfo(true);
        }
    }

    private void onInfoClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogStyle);
        builder.setTitle(R.string.screenshot_image_viewer_dialog_title);
        builder.setAdapter(new InfoItemAdapter(this, mInfoItems), null);
        builder.setPositiveButton(R.string.action_ok, null);
        AlertDialog dialog = builder.create();
        if (dialog.getListView() != null) {
            dialog.getListView().setSelector(android.R.color.transparent);
        }
        dialog.show();
    }

    private void onDeleteClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        builder.setMessage(R.string.screenshot_image_viewer_dialog_delete_msg);
        builder.setPositiveButton(R.string.browsing_history_menu_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TelemetryWrapper.deleteCaptureImage(mScreenshot.getCategory(), mScreenshot.getCategoryVersion());
                proceedDelete();
            }
        });
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.create().show();
    }

    public static String getFileSizeText(long size) {

        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;

        if (size < sizeMo) {
            return df.format(size / sizeKb) + " KB";
        } else if (size < sizeGo) {
            return df.format(size / sizeMo) + " MB";
        } else if (size < sizeTerra) {
            return df.format(size / sizeGo) + " GB";
        }

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
            TextView textTitle = (TextView) convertView.findViewById(R.id.screenshot_info_dialog_text);

            textTitle.setText(item.title);
            if (position == 4) {
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
        if (mScreenshot != null) {
            ThreadUtils.postToBackgroundThread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(mScreenshot.getImageUri());
                    if (file.exists()) {
                        try {
                            file.delete();
                        } catch (Exception ex) {
                        }
                    }
                }
            });
            ScreenshotManager.getInstance().delete(mScreenshot.getId(), this);
        }
    }

    private void showProgressBar(long delayMillis) {
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                if (!mIsImageReady && mProgressBar != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
            }
        };

        handler.postDelayed(r, delayMillis);
    }

    private void hideProgressBar() {
        mIsImageReady = true;
        if (mProgressBar != null && mProgressBar.isShown()) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private static class ScreenshotInfoTask extends AsyncTask<Void, Void, Void> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        private final WeakReference<ScreenshotViewerActivity> activityRef;
        private final Screenshot screenshot;
        private final ArrayList<ImageInfo> infoItems;
        private final boolean withShare;

        private int width;
        private int height;
        private ImageSource imageSource;
        private String fileSizeText;

        public ScreenshotInfoTask(ScreenshotViewerActivity activity, Screenshot screenshot, ArrayList<ImageInfo> infoItems, boolean withShare) {
            this.activityRef = new WeakReference<>(activity);
            this.screenshot = screenshot;
            this.infoItems = infoItems;
            this.withShare = withShare;
        }

        @Override
        protected void onPreExecute() {
            ScreenshotViewerActivity activity = activityRef.get();
            if (activity != null) {
                activity.showProgressBar(DELAY_MILLIS_TO_SHOW_PROGRESS_BAR);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final ScreenshotViewerActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return null;
            }
            // set the category for later use
            screenshot.setCategory(ScreenshotManager.getInstance().getCategory(activity, screenshot.getUrl()));
            screenshot.setCategoryVersion(ScreenshotManager.getInstance().getCategoryVersion());

            final File imgFile = new File(screenshot.getImageUri());
            if (imgFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(screenshot.getImageUri(), options);
                width = options.outWidth;
                height = options.outHeight;

                Uri imageUri = Uri.fromFile(new File(screenshot.getImageUri()));
                imageSource = ImageSource.uri(imageUri);
                fileSizeText = getFileSizeText(imgFile.length());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            ScreenshotViewerActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            if (screenshot.getTimestamp() > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(screenshot.getTimestamp());
                infoItems.get(0).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_time1, dateFormat.format(cal.getTime()));
            }
            infoItems.get(1).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_resolution1, String.format(Locale.getDefault(), "%dx%d", width, height));
            infoItems.get(2).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_size1, fileSizeText);
            infoItems.get(3).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_title1, screenshot.getTitle());
            // UX allow empty string if screenshot is not ready. But this shouldn't happen here.
            infoItems.get(4).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_category, screenshot.getCategory());
            infoItems.get(5).title = activity.getString(R.string.screenshot_image_viewer_dialog_info_url1, screenshot.getUrl());

            if (imageSource != null) {
                activity.mImgScreenshot.setImage(imageSource, ImageViewState.ALIGN_TOP);

                if (withShare) {
                    activity.onShareClick();
                }
            } else {
                activity.hideProgressBar();
                activity.setupView(false);
                Toast.makeText(activity, R.string.message_cannot_find_screenshot, Toast.LENGTH_LONG).show();
            }
        }
    }

    private SubsamplingScaleImageView.OnImageEventListener onImageEventListener = new SubsamplingScaleImageView.OnImageEventListener() {
        @Override
        public void onReady() {
            hideProgressBar();
        }

        @Override
        public void onImageLoaded() {

        }

        @Override
        public void onPreviewLoadError(Exception e) {

        }

        @Override
        public void onImageLoadError(Exception e) {
            hideProgressBar();
        }

        @Override
        public void onTileLoadError(Exception e) {
        }

        @Override
        public void onPreviewReleased() {

        }
    };
}
