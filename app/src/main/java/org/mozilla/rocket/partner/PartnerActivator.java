package org.mozilla.rocket.partner;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.utils.ThreadUtils;
import org.mozilla.httprequest.HttpRequest;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PartnerActivator {

    static final String PARTNER_ACTIVATION_KEY = "ro.vendor.partner";
    //  Sample value could be "moz/1/DEVCFA"

    private static final String PREFERENCE_NAME = "partner_activator";

    private static final String KEY_STRING_STATUS = "string_activation_status";
    private static final String KEY_LONG_FETCH_TIMESTAMP = "long_fetch_timestamp";
    private static final String KEY_LONG_SNOOZE_DURATION = "long_snooze_duration";

    private static final int SOCKET_TAG_PARTNER = 1000;
    private static final String PARTNER_ACTIVATION_SOURCE = "https://firefox.settings.services.mozilla.com/v1/buckets/main/collections/rocket-prefs/records";

    private static Executor executor = Executors.newCachedThreadPool();

    public enum Status {
        Disabled,
        Default,
        Done,
        Snooze
    }

    private final Context context;
    private String[] partnerActivateKeys;
    private Activation activation;

    public PartnerActivator(Context context) {
        this.context = context.getApplicationContext();
    }

    public void launch() {

        postWorker(new QueryActivationStatus(this));

    }

    private void postWorker(Runnable runnable) {
        executor.execute(runnable);
    }

    private boolean statusInvalidate(Status currentStatus) {
        switch (currentStatus) {
            case Disabled:
            case Done:
                PartnerUtil.log("status: " + currentStatus);
                return true;
            case Snooze:
                if (inSnooze()) {
                    PartnerUtil.log("status: inSnooze");
                    return true;
                }
                currentStatus = Status.Default;
                setStatus(currentStatus);
                return false;
            case Default:
            default:
                return false;
        }
    }

    private Status getStatus() {
        Status currentStatus;
        final String statusString = getPreferences(context).getString(KEY_STRING_STATUS, Status.Default.toString());
        if (TextUtils.isEmpty(statusString)) {
            currentStatus = Status.Default;
        } else {
            try {
                currentStatus = Status.valueOf(statusString);
            } catch (Exception e) {
                currentStatus = Status.Default;
            }
        }
        return currentStatus;
    }

    private boolean inSnooze() {
        long lastChecked = getLastFetchedTimestamp();
        long snoozeDuration = getSnoozeDuration();
        long currentTimeMillis = System.currentTimeMillis();

        //  TODO: Assert timestamp boundry
        //  currentTimeMillis is older than app release date
        //  lastChecked is older than app release date or newer than future 5 years
        //  snoozeDuration lower than 3600000
        if (lastChecked <= 0 || snoozeDuration <= 0) {
            return false;
        }

        return lastChecked + snoozeDuration >= currentTimeMillis;
    }

    private void setStatus(Status status) {
        getPreferences(context).edit().putString(KEY_STRING_STATUS, status.toString()).apply();
    }

    private long getSnoozeDuration() {
        return getPreferences(context).getLong(KEY_LONG_SNOOZE_DURATION, 0);
    }

    private void setSnoozeDuration(long duration) {
        getPreferences(context).edit().putLong(KEY_LONG_SNOOZE_DURATION, duration).apply();
    }

    private long getLastFetchedTimestamp() {
        return getPreferences(context).getLong(KEY_LONG_FETCH_TIMESTAMP, 0);
    }

    private void setLastCheckedTimestamp(long timestamp) {
        getPreferences(context).edit().putLong(KEY_LONG_FETCH_TIMESTAMP, timestamp).apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    private static class ActivationJobs {
        final PartnerActivator partnerActivator;

        ActivationJobs(PartnerActivator partnerActivator) {
            this.partnerActivator = partnerActivator;
        }
    }

    private static final class QueryActivationStatus extends ActivationJobs implements Runnable {
        QueryActivationStatus(PartnerActivator partnerActivator) {
            super(partnerActivator);
        }

        @Override
        public void run() {
            final Status currentStatus = partnerActivator.getStatus();
            if (partnerActivator.statusInvalidate(currentStatus)) {
                return;
            }

            String partnerActivateKey = PartnerUtil.getProperty(PARTNER_ACTIVATION_KEY);
            if (TextUtils.isEmpty(partnerActivateKey)) {
                PartnerUtil.log("partner key not found, disabled");
                partnerActivator.setStatus(Status.Disabled);
                return;
            }

            //  TODO: assert partnerActivateKey
            partnerActivator.partnerActivateKeys = partnerActivateKey.split("/");

            if (partnerActivator.partnerActivateKeys == null || partnerActivator.partnerActivateKeys.length != 3) {
                PartnerUtil.log("partner key format invalid");
                return;
            }

            partnerActivator.postWorker(new FetchActivation(partnerActivator, PARTNER_ACTIVATION_SOURCE));
        }
    }

    private static final class FetchActivation extends ActivationJobs implements Runnable {

        //  TODO: update useragent string
        private static final String UserAgentString = "";
        private static final int HTTP_REQUEST_TIMEOUT = 30000;

        private final String sourceUrl;
        private final String[] activationKeys;

        FetchActivation(PartnerActivator partnerActivator, String sourceUrl) {
            super(partnerActivator);
            this.sourceUrl = sourceUrl;
            this.activationKeys = partnerActivator.partnerActivateKeys;
        }

        @Override
        public void run() {
            try {
                TrafficStats.setThreadStatsTag(SOCKET_TAG_PARTNER);

                URL request = new URL(sourceUrl);
                String json = HttpRequest.get(request, HTTP_REQUEST_TIMEOUT, UserAgentString);

                JSONArray activationJsonArray = null;
                JSONArray jsonArray = new JSONObject(json).getJSONArray("data");
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject object = (JSONObject) jsonArray.get(i);
                        activationJsonArray = object.getJSONObject("partner").getJSONArray("activation");
                    } catch (JSONException e) {
                        PartnerUtil.log(e, "FetchActivation source json format error");
                    }
                }
                if (activationJsonArray == null) {
                    PartnerUtil.log("FetchActivation activation json not found");
                    return;
                }

                Activation found = null;
                for (int i = 0, size = activationJsonArray.length(); i < size; i++) {
                    JSONObject activationJsonObject = (JSONObject) activationJsonArray.get(i);
                    Activation activation = Activation.from(activationJsonObject);
                    if (activation.matchKeys(activationKeys)) {
                        found = activation;
                        break;
                    }
                }

                if (found == null) {
                    //  Activation Not Found
                    PartnerUtil.log("FetchActivation activation not found, disabled");
                    partnerActivator.setStatus(Status.Disabled);
                    return;
                }

                partnerActivator.activation = found;

                if (found.duration != 0) {
                    partnerActivator.setSnoozeDuration(found.duration);
                }
                if (partnerActivator.inSnooze()) {
                    partnerActivator.setStatus(Status.Snooze);
                    PartnerUtil.log("FetchActivation update snoozed");
                    return;
                } else {
                    partnerActivator.setLastCheckedTimestamp(System.currentTimeMillis());
                }

                partnerActivator.postWorker(new PingActivation(partnerActivator));

            } catch (Exception e) {
                PartnerUtil.log(e, "FetchActivation Exception");
            } finally {
                TrafficStats.clearThreadStatsTag();
            }
        }
    }

    private static class PingActivation extends ActivationJobs implements Runnable {

        PingActivation(PartnerActivator partnerActivator) {
            super(partnerActivator);
        }

        private static final String TYPE_STRING = "stringValue";
        private static final String TYPE_INTEGER = "integerValue";

        @Override
        public void run() {
            Activation activation = partnerActivator.activation;

            URL url = null;
            try {
                url = new URL(activation.url);
            } catch (MalformedURLException e) {
                PartnerUtil.log(e, "PingActivation URL malformed");
            }

            if (url == null) {
                PartnerUtil.log("PingActivation URL not found");
                return;
            }

            try {
                TrafficStats.setThreadStatsTag(SOCKET_TAG_PARTNER);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(60000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                urlConnection.setRequestProperty("Content-Type", "application/json");


                JSONObject root = new JSONObject();
                JSONObject fields = new JSONObject();
                root.put("fields", fields);
                fields.put("device_id", new JSONObject().put(TYPE_STRING, PartnerUtil.getDeviceIdentifier(partnerActivator.context.getContentResolver())));
                fields.put("owner", new JSONObject().put(TYPE_STRING, activation.owner));
                fields.put("version", new JSONObject().put(TYPE_INTEGER, activation.version));
                fields.put("manufacture", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.product.manufacturer")));
                fields.put("model", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.product.model")));
                fields.put("name", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.product.name")));
                fields.put("device", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.product.device")));
                fields.put("brand", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.product.brand")));
                fields.put("build_id", new JSONObject().put(TYPE_STRING, PartnerUtil.getProperty("ro.build.id")));


                String str = root.toString();
                byte[] outputBytes = str.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = urlConnection.getOutputStream()) {
                    os.write(outputBytes);
                    os.flush();
                } catch (Exception e) {
                    PartnerUtil.log(e, "PingActivation post exception");
                    throw e;
                }

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    //  TODO: Telemetry ping fail
                    PartnerUtil.log("PingActivation server response not OK: " + responseCode);
                } else {
                    partnerActivator.setStatus(Status.Done);
                }

            } catch (Exception e) {
                PartnerUtil.log(e, "PingActivation Exception");
            } finally {
                TrafficStats.clearThreadStatsTag();
            }

        }
    }
}
