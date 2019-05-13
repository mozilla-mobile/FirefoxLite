package org.mozilla.focus.home;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Consumer;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.banner.BannerAdapter;
import org.mozilla.banner.OnClickListener;
import org.mozilla.banner.TelemetryListener;
import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.httptask.SimpleLoadUrlTask;
import org.mozilla.rocket.util.LoggerWrapper;
import org.mozilla.threadutils.ThreadUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

class BannerHelper {

    interface HomeBannerHelperListener {
        void showHomeBannerProcedure(BannerAdapter b);
        void hideHomeBannerProcedure(Void v);
        OnClickListener onBannerClickListener();
    }

    private static String TAG = "BannerHelper";

    private String[] homeBannerconfigArray;

    @Nullable private HomeBannerHelperListener listener;

    public void setListener(@Nullable HomeBannerHelperListener listener) {
        this.listener = listener;
    }

    private static class LoadRootConfigTask extends SimpleLoadUrlTask {

        private AtomicInteger countdown;
        private String userAgent;

        private interface OnRootConfigLoadedListener {
            void onRootConfigLoaded(String[] configArray);
        }

        OnRootConfigLoadedListener onRootConfigLoadedListener;

        LoadRootConfigTask(OnRootConfigLoadedListener onRootConfigLoadedListener) {
            this.onRootConfigLoadedListener = onRootConfigLoadedListener;
        }

        @Override
        protected String doInBackground(String... strings) {
            // Intercept UA;
            userAgent = strings[1];
            return super.doInBackground(strings);
        }

        @Override
        protected void onPostExecute(String line) {
            // Improper root Manifest Url;
            if (line == null || TextUtils.isEmpty(line)) {
                return;
            }
            try {
                JSONArray jsonArray = new JSONArray(line);
                int length = jsonArray.length();
                String[] configArray = new String[length];
                countdown = new AtomicInteger(length);
                LoadConfigTask.OnConfigLoadedListener onConfigLoadedListener = (config, index) -> {
                    configArray[index] = config;
                    if (countdown.decrementAndGet() == 0) {
                        onRootConfigLoadedListener.onRootConfigLoaded(configArray);
                    }
                };
                for (int i = 0; i < length; i++) {
                    new LoadConfigTask(new WeakReference<>(onConfigLoadedListener), i).execute(jsonArray.getString(i), userAgent, Integer.toString(SocketTags.BANNER));
                }
            } catch (JSONException e) {
                onRootConfigLoadedListener.onRootConfigLoaded(null);
            }
        }
    }

    private static class LoadConfigTask extends SimpleLoadUrlTask {

        private interface OnConfigLoadedListener {
            void onConfigLoaded(String config, int index);
        }

        private WeakReference<OnConfigLoadedListener> onConfigLoadedListenerRef;
        private int index;

        LoadConfigTask(WeakReference<OnConfigLoadedListener> onConfigLoadedListenerRef, int index) {
            this.onConfigLoadedListenerRef = onConfigLoadedListenerRef;
            this.index = index;
        }

        @Override
        protected void onPostExecute(String line) {
            // Trim \n \r since these will not be written to cache.
            line = line.replace("\n", "").replace("\r", "");
            OnConfigLoadedListener onConfigLoadedListener = onConfigLoadedListenerRef.get();
            if (onConfigLoadedListener != null) {
                onConfigLoadedListener.onConfigLoaded(line, index);
            }
        }
    }

    void initHomeBanner(Context context, MutableLiveData<String[]> configLiveData) {
        if (listener != null) {
            initBanner(context, AppConfigWrapper.getBannerRootConfig(), CURRENT_HOME_BANNER_CONFIG, configLiveData, listener::hideHomeBannerProcedure);
        }
    }

    void initCouponBanner(Context context, MutableLiveData<String[]> configLiveData) {
        initBanner(context, AppConfigWrapper.getCouponBannerRootConfig(), CURRENT_COUPON_BANNER_CONFIG, configLiveData, null);
    }


    // TODO: 10/3/18 Now we have cachedrequestloader, should consider migrate to use it.
    private void initBanner(Context context, String manifest, String cacheName, MutableLiveData<String[]> configLiveData, Consumer<Void> hideBannerProcedure) {
        // Setup from Cache
        try {
            new FileUtils.ReadStringFromFileTask<>(new FileUtils.GetCache(new WeakReference<>(context)).get(), cacheName, configLiveData, BannerHelper::stringToStringArray).execute();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open Cache directory when reading cached banner config");
        }
        // Setup from Network
        if (TextUtils.isEmpty(manifest)) {
            deleteCache(context, cacheName);
            if (hideBannerProcedure != null) {
                hideBannerProcedure.accept(null);
            }
        } else {
            Callback callback = new Callback(context, cacheName, configLiveData);
            new LoadRootConfigTask(callback).execute(manifest, WebViewProvider.getUserAgentString(context), Integer.toString(SocketTags.BANNER));
        }
    }

    private static class Callback implements LoadRootConfigTask.OnRootConfigLoadedListener {

        private WeakReference<Context> contextRef;
        private String cacheName;
        private MutableLiveData<String[]> configLiveData;

        Callback(Context context, String cacheName, MutableLiveData<String[]> configLiveData) {
            this.contextRef = new WeakReference<>(context);
            this.cacheName = cacheName;
            this.configLiveData = configLiveData;
        }

        @Override
        public void onRootConfigLoaded(String[] configArray) {
            Context context = contextRef.get();
            if (context != null) {
                writeToCache(context, configArray, cacheName);
                configLiveData.setValue(configArray);
            }
        }
    }

    void setUpHomeBannerFromConfig(String[] configArray) {
        TelemetryListener bannerInnerTelemetryListener = new TelemetryListener() {
            @Override
            public void sendClickItemTelemetry(String jsonString, int itemPosition) {
                try {
                    TelemetryWrapper.clickBannerItem(new JSONObject(jsonString).getString("id"), itemPosition);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void sendClickBackgroundTelemetry(String jsonString) {
                try {
                    TelemetryWrapper.clickBannerBackground(new JSONObject(jsonString).getString("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        if (listener != null) {
            setUpBannerFromConfig(configArray, this::updateHomeConfig, homeBannerconfigArray, bannerInnerTelemetryListener, listener::hideHomeBannerProcedure, listener::showHomeBannerProcedure);
        }
    }

    private void updateHomeConfig(String[] configArray) {
        homeBannerconfigArray = configArray;
    }

    private void setUpBannerFromConfig(String[] configArray, Consumer<String[]> configUpdater, String[] oldConfigArray, TelemetryListener telemetryListener,  Consumer<Void> hideBannerConsumer, Consumer<BannerAdapter> showBannerConsumer) {
        if (Arrays.equals(oldConfigArray, configArray)) {
            return;
        }
        boolean isUpdate = oldConfigArray != null;
        configUpdater.accept(configArray);
        if (configArray == null || configArray.length == 0) {
            hideBannerConsumer.accept(null);
            return;
        }
        try {
            if (listener == null) {
                return;
            }
            BannerAdapter bannerAdapter = new BannerAdapter(configArray, listener.onBannerClickListener(), telemetryListener);
            showBannerConsumer.accept(bannerAdapter);
            if (isUpdate) {
                TelemetryWrapper.showBannerNew(bannerAdapter.getFirstDAOId());
            } else {
                TelemetryWrapper.showBannerUpdate(bannerAdapter.getFirstDAOId());
            }

        } catch (JSONException e) {
            LoggerWrapper.throwOrWarn(TAG, "Invalid Config: " + e.getMessage());
        }
    }

    private static void writeToCache(Context context, String[] configArray, String cacheName) {
        try {
            final Runnable runnable = new FileUtils.WriteStringToFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), cacheName), stringArrayToString(configArray));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open cache directory when writing banner config to cache");
        }
    }

    private void deleteCache(Context context, String cacheName) {
        try {
            final Runnable runnable = new FileUtils.DeleteFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), cacheName));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open cache directory when deleting banner cache");
        }
    }

    private static final String UNIT_SEPARATOR = Character.toString((char) 0x1F);
    // Please don't rename the string
    private static final String CURRENT_HOME_BANNER_CONFIG = "CURRENT_BANNER_CONFIG";
    private static final String CURRENT_COUPON_BANNER_CONFIG = "CURRENT_COUPON_BANNER_CONFIG";

    private static String stringArrayToString(String[] stringArray) {
        return TextUtils.join(UNIT_SEPARATOR, stringArray);
    }

    private static String[] stringToStringArray(String string) {
        if (TextUtils.isEmpty(string)) {
            return new String[]{};
        }
        return string.split(UNIT_SEPARATOR);
    }

}