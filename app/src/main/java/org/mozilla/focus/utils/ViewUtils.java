/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.mozilla.focus.R;

public class ViewUtils {


    public final static int SYSTEM_UI_VISIBILITY_NONE = -1;

    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x01000000;


    public static void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    /**
     * Create a snackbar with Focus branding (See #193).
     */
    public static void showBrandedSnackbar(View view, @StringRes int resId, int delayMillis) {
        final Context context = view.getContext();
        final Snackbar snackbar = Snackbar.make(view, resId, Snackbar.LENGTH_LONG);

        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(context, R.color.snackbarBackground));

        final TextView snackbarTextView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setTextColor(ContextCompat.getColor(context, R.color.snackbarTextColor));
        snackbarTextView.setGravity(Gravity.CENTER);
        snackbarTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                snackbar.show();
            }
        }, delayMillis);
    }

    public static boolean isRTL(View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Update the alpha value of the view with the given id. All kinds of failures (null activity,
     * view not found, ..) will be ignored by this method.
     */
    public static void updateAlphaIfViewExists(@Nullable Activity activity, @IdRes int id, float alpha) {
        if (activity == null) {
            return;
        }

        final View view = activity.findViewById(id);
        if (view == null) {
            return;
        }

        view.setAlpha(alpha);
    }

    public static int getStatusBarHeight(@NonNull final Activity activity) {
        final Rect rectangle = new Rect();
        final Window window = activity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.top;
    }

    /**
     * Hide system bars. They can be revealed temporarily with system gestures, such as swiping from
     * the top of the screen. These transient system bars will overlay app’s content, may have some
     * degree of transparency, and will automatically hide after a short timeout.
     */
    public static int switchToImmersiveMode(final Activity activity) {
        if (activity == null) {
            return SYSTEM_UI_VISIBILITY_NONE;
        }
        Window window = activity.getWindow();
        final int original = window.getDecorView().getSystemUiVisibility();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        return original;
    }

    /**
     * Show the system bars again.
     */
    public static void exitImmersiveMode(int visibility, final Activity activity) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setSystemUiVisibility(visibility);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public static void updateStatusBarStyle(final boolean isLight, final Window window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        int flags = window.getDecorView().getSystemUiVisibility();
        if (!isLight) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }
}
