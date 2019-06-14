package org.mozilla.focus.screengrab;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.SystemClock;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;

public class MockUIUtils {

    static final int LONG_DELAY = 3500; // 3.5 seconds
    static final int SHORT_DELAY = 2000; // 2 seconds
    static final int POPUP_DELAY = 800;

    static void showSnackbarAndWait(@NotNull Activity activity, int strId, int actionStrId) {
        View view = activity.findViewById(R.id.container);
        if (view != null) {
            Snackbar.make(view, strId, Snackbar.LENGTH_LONG).setAction(actionStrId, v -> { }).show();
            SystemClock.sleep(POPUP_DELAY);
        }
    }

    static void showToast(@NotNull Activity activity, int strId) {
        activity.runOnUiThread(() -> Toast.makeText(activity, strId, Toast.LENGTH_LONG).show());
        // Since we post a show toast runnable to main thread, we delay for a while to make sure toast is displayed
        SystemClock.sleep(POPUP_DELAY);
    }

    static void showGeoPromptDialog(@NotNull Activity activity, String url) {
        activity.runOnUiThread(() -> {
            AlertDialog dialog = ((MainActivity) activity).getVisibleBrowserFragment().buildGeoPromptDialog();
            dialog.show();
        });
    }
}
