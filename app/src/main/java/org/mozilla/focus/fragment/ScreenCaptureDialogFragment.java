/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;

import org.mozilla.focus.R;

public class ScreenCaptureDialogFragment extends DialogFragment {

    private LottieAnimationView mLottieAnimationView;

    private static final float KEYFRAME_BEGIN = 0f;
    private static final float KEYFRAME_LOOP = 0.6f;
    private static final float KEYFRAME_END = 1f;

    public static ScreenCaptureDialogFragment newInstance() {
        ScreenCaptureDialogFragment screenCaptureDialogFragment = new ScreenCaptureDialogFragment();
        screenCaptureDialogFragment.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.ScreenCaptureDialog);
        return screenCaptureDialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_capture_screen, container, false);
        mLottieAnimationView = (LottieAnimationView) view.findViewById(R.id.animation_view);
        mLottieAnimationView.addAnimatorListener(new AnimatorListenerAdapter() {

            boolean playClosing = true;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (playClosing) {
                    playClosing = false;
                    mLottieAnimationView.post(new Runnable() {
                        @Override
                        public void run() {
                            mLottieAnimationView.playAnimation(KEYFRAME_LOOP, KEYFRAME_END);
                        }
                    });
                } else {
                    mLottieAnimationView.post(new Runnable() {
                        @Override
                        public void run() {
                            dismissAllowingStateLoss();
                        }
                    });
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                dismiss();
            }
        });
        return view;
    }

    public void start() {
        mLottieAnimationView.playAnimation(KEYFRAME_BEGIN, KEYFRAME_LOOP);
    }

    public void dismiss(boolean waitAnimation) {
        if (waitAnimation) {
            mLottieAnimationView.loop(false);
        } else {
            dismissAllowingStateLoss();
        }
    }

}
