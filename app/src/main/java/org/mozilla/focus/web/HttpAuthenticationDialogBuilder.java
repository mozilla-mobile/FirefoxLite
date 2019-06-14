/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import org.mozilla.focus.R;

public class HttpAuthenticationDialogBuilder {

    private final Context context;

    private final String host;
    private final String realm;

    private AlertDialog dialog;
    private TextView usernameTextView;
    private TextView passwordTextView;

    private OkListener okListener;
    private CancelListener cancelListener;

    public static class Builder {
        private final Context context;
        private final String host;
        private final String realm;

        private OkListener okListener;
        private CancelListener cancelListener;

        public Builder(Context context, String host, String realm) {
            this.context = context;
            this.host = host;
            this.realm = realm;
        }

        public Builder setOkListener(OkListener okListener) {
            this.okListener = okListener;
            return this;
        }

        public Builder setCancelListener(CancelListener cancelListener) {
            this.cancelListener = cancelListener;
            return this;
        }

        public HttpAuthenticationDialogBuilder build() {
            return new HttpAuthenticationDialogBuilder(this);
        }
    }

    HttpAuthenticationDialogBuilder(Builder builder) {
        context = builder.context;
        host = builder.host;
        realm = builder.realm;
        okListener = builder.okListener;
        cancelListener = builder.cancelListener;
    }


    private String getUsername() {
        return usernameTextView.getText().toString();
    }

    private String getPassword() {
        return passwordTextView.getText().toString();
    }

    public void show() {
        dialog.show();
        int photonBlue = ContextCompat.getColor(context, R.color.paletteDarkBlueC100);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(photonBlue);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(photonBlue);
        usernameTextView.requestFocus();
    }


    public void createDialog() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_http_auth, null);
        usernameTextView = view.findViewById(R.id.httpAuthUsername);
        passwordTextView = view.findViewById(R.id.httpAuthPassword);
        passwordTextView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        buildDialog(view);
    }

    private void buildDialog(View view) {
        dialog = new AlertDialog.Builder(context)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setView(view)
                .setPositiveButton(R.string.action_ok, (dialog, whichButton) -> {
                    if (okListener != null) {
                        okListener.onOk(host, realm, getUsername(), getPassword());
                    }
                })
                .setNegativeButton(R.string.action_cancel, (dialog, whichButton) -> {
                    if (cancelListener != null) {
                        cancelListener.onCancel();
                    }
                })
                .setOnCancelListener(dialog -> {
                    if (cancelListener != null) {
                        cancelListener.onCancel();
                    }
                })
                .create();
        final Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    public interface OkListener {
        void onOk(String host, String realm, String username, String password);
    }

    public interface CancelListener {
        void onCancel();
    }
}
