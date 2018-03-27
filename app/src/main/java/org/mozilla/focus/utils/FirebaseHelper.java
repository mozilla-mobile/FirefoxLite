/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;

//import com.firebase.ui.auth.AuthUI;
//import com.google.firebase.auth.AuthCredential;
//import com.google.firebase.auth.EmailAuthProvider;

/**
 * Provide helper for Firebase functionality
 */
public class FirebaseHelper {

    public static final String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    public static final String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";
    private static final String TAG = "FirebaseHelper";

    private static WeakReference<FirebaseRemoteConfig> REMOTE_CONFIG;
    private static long CACHE_EXPIRATION = 3600; // 1 hour in seconds.

    public static final int REQUEST_SIGN_IN = 1;


    public static String getString(String key) {
        final FirebaseRemoteConfig config = REMOTE_CONFIG.get();
        if (config != null) {
            return config.getString(key);
        }
        return null;
    }

    public static void init(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                internalInit(context);
            }
        }).start();
    }

    private static void internalInit(final Context context) {

        final FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        REMOTE_CONFIG = new WeakReference<>(config);
        final FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        config.setConfigSettings(configSettings);
        config.setDefaults(getRemoteConfigDefault(context));

        // If app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (config.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            CACHE_EXPIRATION = 0;
        }
        refresh();
    }

    public static void checkIfInstallFromReferral(Context context, Intent intent) {
        final boolean isActivity = context instanceof Activity;
        if (!isActivity) {
            return;
        }
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(intent)
                .addOnSuccessListener((Activity) context, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }
                        //
                        // If the user isn't signed in and the pending Dynamic Link is
                        // an invitation, sign in the user anonymously, and record the
                        // referrer's UID.
                        //
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null
                                && deepLink != null
                                && deepLink.getQueryParameter("invitedby") != null) {
                            String referrerUid = deepLink.getQueryParameter("invitedby");
                            createAnonymousAccountWithReferrerInfo(referrerUid);
                        }
                    }
                });
    }

    // Call this method to refresh the value in remote config
    public static void refresh() {
        final FirebaseRemoteConfig config = REMOTE_CONFIG.get();
        if (config == null) {
            return;
        }
        config.fetch(CACHE_EXPIRATION).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Successfully ");
                    config.activateFetched();
                } else {
                    Log.d(TAG, "Firebase RmoteConfig Fetch Failed: ");
                }

            }
        });
    }

    public static HashMap<String, Object> getRemoteConfigDefault(Context context) {

        final HashMap<String, Object> map = new HashMap<>();
        map.put(RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title));
        map.put(RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
        return map;
    }

    public static Task<ShortDynamicLink> shareWithDynamicLink(final Context context, final Dialog dialog) {
        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://example.com/"))
                .setDynamicLinkDomain("w662p.app.goo.gl")
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
                .setGoogleAnalyticsParameters(
                        new DynamicLink.GoogleAnalyticsParameters.Builder()
                                .setSource("orkut")
                                .setMedium("social")
                                .setCampaign("example-promo")
                                .build())
                .setSocialMetaTagParameters(
                        new DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle("Example of a Dynamic Link")
                                .setDescription("This link works whether the app is installed or not!")
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener(new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            // Short link created
                            final ShortDynamicLink link = task.getResult();
                            Uri shortLink = link.getShortLink();
                            Uri flowchartLink = link.getPreviewLink();

                            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.setType("text/plain");
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
//                            sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_promotion_text));
                            final String text = String.format("Hey, check out this fast and lightweight browser from Mozilla. %s", shortLink);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, text);

                            context.startActivity(Intent.createChooser(sendIntent, null));
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                        } else {
                            // Error
                            // ...
                        }
                    }
                });
        return shortLinkTask;
    }

    public static void checkIfNeedToShowRegister(Activity activity) {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser.isAnonymous()) {∂
//            List<AuthUI.IdpConfig> providers = Arrays.asList(
//                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build());
//            activity.startActivityForResult(
//                    AuthUI.getInstance()
//                            .createSignInIntentBuilder()
//                            .setAvailableProviders(providers)
//                            .build(),
//                    REQUEST_SIGN_IN);
//        }


    }

    private static void createAnonymousAccountWithReferrerInfo(final String referrerUid) {

        FirebaseAuth.getInstance()
                .signInAnonymously()
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Keep track of the referrer in the RTDB. Database calls
                        // will depend on the structure of your app's RTDB.
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        DatabaseReference userRecord =
                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .child(user.getUid());
                        userRecord.child("referred_by").setValue(referrerUid);
                    }
                });
    }

    public static void handleSiginResult(Intent data, int resultCode) {
        if (resultCode == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            // ..
            DatabaseReference userRecord =
                    FirebaseDatabase.getInstance().getReference()
                            .child("users")
                            .child(user.getUid());
            userRecord.child("last_signin_at").setValue(ServerValue.TIMESTAMP);
        } else {
            // Sign in failed, check response for error code
            // ...
        }
    }
}
