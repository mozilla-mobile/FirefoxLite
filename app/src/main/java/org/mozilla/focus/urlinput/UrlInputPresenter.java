/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class UrlInputPresenter implements UrlInputContract.Presenter {

    private UrlInputContract.View view;

    // This is just a Mock Presenter, in real implementation we should get rid of Android classes.
    final private Context ctx;
    final private Handler handler;
    final private static int SUGGESTION_TAG = 0x9527;
    final private static long DELAY = 1000;

    public UrlInputPresenter(@NonNull Context context) {
        this.ctx = context;
        this.handler = new MyHandler(this.ctx.getMainLooper());
    }

    @Override
    public void setView(UrlInputContract.View view) {
        this.view = view;
    }

    @MainThread
    @Override
    public void onInput(CharSequence input) {
        if (view == null) {
            return;
        }

        if (this.handler.hasMessages(SUGGESTION_TAG)) {
            this.handler.removeMessages(SUGGESTION_TAG);
        }

        if (input.length() == 0) {
            this.view.setSuggestions(null);
            return;
        }

        Message msg = this.handler.obtainMessage(SUGGESTION_TAG);
        msg.obj = input;
        this.handler.sendMessageDelayed(msg, DELAY);
    }

    class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (view == null) {
                return;
            }
            final CharSequence input = (CharSequence) msg.obj;

            final String[] append = {"foo", "bar", "firefox",
                    " test", "mozilla", "zerda",
                    " internet", " taiwan", " japan"};
            List<CharSequence> texts = new ArrayList<>();
            for (int i = 0; i < append.length; i++) {
                texts.add(input + " " + append[i]);
            }

            UrlInputPresenter.this.view.setSuggestions(texts);
        }
    }
}
