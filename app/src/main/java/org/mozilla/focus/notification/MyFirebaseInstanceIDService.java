/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

// This class used to refresh the client's fcm token and send it back to our backend.
// But since we are not using any own backend, this class is not useful here.
// I left it here if anyone want to continue the work here.
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "MyFirebaseIIDService";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
    }
}