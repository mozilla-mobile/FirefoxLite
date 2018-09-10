package org.mozilla.rocket.distribution;

import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.httprequest.HttpRequest;
import org.mozilla.rocket.util.ForeGroundIntentService;
import org.mozilla.rocket.util.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class LoadDistributionConfigService extends ForeGroundIntentService {

    public static final String MESSENGER_INTENT_KEY = "messenger_activity";
    public static final String MESSAGE_ID_INTENT_KEY = "message_id";
    private Messenger activityMessenger;
    private static final String TAG = "LoadDistributionConfigService";
    private static final String CHANNEL_ID = "load_distribution_service";

    private int onCompleteMessageId;
    private int failureCount = 0;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public LoadDistributionConfigService() {
        super("LoadDistributionConfigService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        final boolean canNotify = intent.hasExtra(MESSENGER_INTENT_KEY) && intent.hasExtra(MESSAGE_ID_INTENT_KEY);
        startForeground();
        activityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY);
        onCompleteMessageId = intent.getIntExtra(MESSAGE_ID_INTENT_KEY, /* Don't care */-1);
        queryDistributionUri(canNotify);
        stopForeground();
    }

    private void queryDistributionUri(boolean canNotify) {
        final Context applicationContext = getApplicationContext();
        TrafficStats.setThreadStatsTag(SocketTags.DISTRIBUTION);
        final String uri = AppConfigWrapper.getCustomTopSitesUri(applicationContext);
        final long version = AppConfigWrapper.getCustomTopSitesVersion(applicationContext);
        if (version == 1) {
            final JSONArray configJson;
            if (uri == null) {
                return;
            }
            final String config;
            // Invalid http url
            try {
                config = HttpRequest.get(new URL(uri), WebViewProvider.getUserAgentString(applicationContext));
            } catch (MalformedURLException ignored) {
                ignored.printStackTrace();
                Logger.throwOrWarn(TAG, "Invalid url");
                return;
            }
            // Http failure
            if (TextUtils.isEmpty(config)) {
                if (failureCount < 3) {
                    failureCount++;
                    queryDistributionUri(canNotify);
                }
                return;
            }
            // Invalid json response
            try {
                configJson = new JSONArray(config);
            } catch (JSONException ignored) {
                ignored.printStackTrace();
                Logger.throwOrWarn(TAG, "Invalid distribution bootstrap file");
                return;
            }
            new Handler().post(() -> {
                // Done on main thread so shared preference is synced.
                TopSitesUtils.addLastViewTimeStamp(configJson);
                TopSitesUtils.saveDefaultSites(applicationContext, configJson);
                if (canNotify) {
                    Message m = Message.obtain();
                    m.what = onCompleteMessageId;
                    try {
                        activityMessenger.send(m);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("Unknown Activation version when parsing customization.");
        }
    }

    @Override
    protected String getNotificationId() {
        return CHANNEL_ID;
    }
}
