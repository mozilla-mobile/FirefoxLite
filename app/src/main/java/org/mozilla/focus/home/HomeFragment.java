/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQueryBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.provider.HistoryContract;
import org.mozilla.focus.provider.HistoryDatabaseHelper;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.tabs.TabCounter;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConfigWrapper;
import org.mozilla.focus.utils.DimenUtils;
import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.focus.utils.OnSwipeListener;
import org.mozilla.focus.utils.RemoteConfigConstants;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SwipeMotionDetector;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.SwipeMotionLayout;
import org.mozilla.httptask.SimpleLoadUrlTask;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.lite.partner.NewsItem;
import org.mozilla.rocket.banner.BannerAdapter;
import org.mozilla.rocket.banner.BannerConfigViewModel;
import org.mozilla.rocket.banner.BannerViewHolder;
import org.mozilla.rocket.content.NewsPresenter;
import org.mozilla.rocket.content.ContentPortalView;
import org.mozilla.rocket.content.NewsViewContract;
import org.mozilla.rocket.download.DownloadIndicatorViewModel;
import org.mozilla.rocket.home.pinsite.PinSiteManager;
import org.mozilla.rocket.home.pinsite.PinSiteManagerKt;
import org.mozilla.rocket.nightmode.themed.ThemedImageButton;
import org.mozilla.rocket.nightmode.themed.ThemedTextView;
import org.mozilla.rocket.persistance.History.HistoryDatabase;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabViewClient;
import org.mozilla.rocket.tabs.TabViewEngineSession;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.theme.ThemeManager;
import org.mozilla.rocket.util.LoggerWrapper;
import org.mozilla.threadutils.ThreadUtils;
import org.mozilla.urlutils.UrlUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends LocaleAwareFragment implements TopSitesContract.View, TopSitesContract.Model,
        ScreenNavigator.HomeScreen, NewsViewContract {
    private static final String TAG = "HomeFragment";

    public static final String TOPSITES_PREF = "topsites_pref";
    public static final String TOP_SITES_V2_PREF = "top_sites_v2_complete";
    public static final int TOP_SITES_QUERY_LIMIT = 8;
    public static final int TOP_SITES_QUERY_MIN_VIEW_COUNT = 6;
    private static final int MSG_ID_REFRESH = 8269;

    private static final float ALPHA_TAB_COUNTER_DISABLED = 0.3f;
    public static final String BANNER_MANIFEST_DEFAULT = "";

    private TopSitesContract.Presenter presenter;
    private RecyclerView recyclerView;
    private ThemedImageButton btnMenu;
    @Nullable private ImageButton arrow1;
    @Nullable private ImageButton arrow2;
    @Nullable private ContentPortalView contentPanel;

    private View themeOnboardingLayer;
    private TabCounter tabCounter;
    private ThemedTextView fakeInput;
    private HomeScreenBackground homeScreenBackground;
    private SiteItemClickListener clickListener = new SiteItemClickListener();
    private TopSiteAdapter topSiteAdapter;
    private JSONArray orginalDefaultSites = null;
    private SessionManager sessionManager;
    private final SessionManagerObserver observer = new SessionManagerObserver();
    private RecyclerView banner;
    private LinearLayoutManager bannerLayoutManager;
    private BroadcastReceiver receiver;
    private LoadRootConfigTask.OnRootConfigLoadedListener onRootConfigLoadedListener;
    private Timer timer;
    private static final int SCROLL_PERIOD = 10000;
    private BannerConfigViewModel bannerConfigViewModel;
    final Observer<String[]> bannerObserver = this::setUpBannerFromConfig;
    private String[] configArray;
    private LottieAnimationView downloadingIndicator;
    private ImageView downloadIndicator;
    @Nullable
    private NewsPresenter newsPresenter = null;
    private PinSiteManager pinSiteManager;

    private Handler uiHandler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(Message msg) {
            if (msg.what == MSG_ID_REFRESH) {
                refreshTopSites();
            }
        }
    };

    public static HomeFragment create() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.presenter = new TopSitesPresenter();
        this.presenter.setView(this);
        this.presenter.setModel(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        showCurrentBannerTelemetry();
        TelemetryWrapper.showHome();
    }

    private void showCurrentBannerTelemetry() {
        if (banner.getVisibility() != View.VISIBLE || bannerLayoutManager == null) {
            return;
        }
        // Since we're using SnapHelper, the only shown child would be at position 0
        final int displayedChildPosition = 0;
        View displayedView = banner.getChildAt(displayedChildPosition);
        if (displayedView == null) {
            return;
        }
        String id = ((BannerViewHolder) banner.getChildViewHolder(displayedView)).getId();
        if (id == null) {
            return;
        }
        TelemetryWrapper.showBannerReturn(id);
    }

    private void showBanner(boolean enabled) {
        if (enabled) {
            banner.setVisibility(View.VISIBLE);
        } else {
            banner.setVisibility(View.GONE);
        }
    }

    @Override
    public Fragment getFragment() {
        return this;
    }


    // return true if there's a content portal to hide
    public boolean hideContentPortal() {
        if (contentPanel != null) {
            return contentPanel.hide();
        }
        return false;
    }

    @Override
    public List<Site> getSites() {
        return presenter.getSites();
    }

    @Override
    public void pinSite(Site site, Runnable onUpdateComplete) {
        pinSiteManager.pin(site);
        onUpdateComplete.run();
    }

    @Override
    public void updateNews(@Nullable List<? extends NewsItem> items) {
        contentPanel.setNewsContent(items);
    }

    private static class LoadRootConfigTask extends SimpleLoadUrlTask {

        private AtomicInteger countdown;
        private String userAgent;

        private interface OnRootConfigLoadedListener {
            void onRootConfigLoaded(String[] configArray);
        }

        WeakReference<OnRootConfigLoadedListener> onRootConfigLoadedListenerRef;

        LoadRootConfigTask(WeakReference<OnRootConfigLoadedListener> onConfigLoadedListenerRef) {
            this.onRootConfigLoadedListenerRef = onConfigLoadedListenerRef;
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
            OnRootConfigLoadedListener onRootConfigLoadedListener = onRootConfigLoadedListenerRef.get();
            if (onRootConfigLoadedListener == null) {
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

    // TODO: 10/3/18 Now we have cachedrequestloader, should consider migrate to use it.
    private void initBanner(Context context) {
        // Setup from Cache
        try {
            new FileUtils.ReadStringFromFileTask<>(new FileUtils.GetCache(new WeakReference<>(context)).get(), CURRENT_BANNER_CONFIG, bannerConfigViewModel.getConfig(), HomeFragment::stringToStringArray).execute();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open Cache directory when reading cached banner config");
        }
        // Setup from Network
        String manifest = AppConfigWrapper.getBannerRootConfig();
        if (TextUtils.isEmpty(manifest)) {
            deleteCache(context);
            banner.setAdapter(null);
            showBanner(false);
        } else {
            // Not using a local variable to prevent reference to be cleared before returning.
            onRootConfigLoadedListener = configArray -> {
                writeToCache(context, configArray);
                bannerConfigViewModel.getConfig().setValue(configArray);
                onRootConfigLoadedListener = null;
            };
            new LoadRootConfigTask(new WeakReference<>(onRootConfigLoadedListener)).execute(manifest, WebViewProvider.getUserAgentString(getActivity()), Integer.toString(SocketTags.BANNER));
        }
    }

    private void setUpBannerFromConfig(String[] configArray) {
        if (Arrays.equals(this.configArray, configArray)) {
            return;
        }
        boolean isUpdate = this.configArray != null;
        this.configArray = configArray;
        if (configArray == null || configArray.length == 0) {
            showBanner(false);
            return;
        }
        try {
            BannerAdapter bannerAdapter = new BannerAdapter(configArray, arg -> FragmentListener.notifyParent(this, FragmentListener.TYPE.OPEN_URL_IN_NEW_TAB, arg));
            banner.setAdapter(bannerAdapter);
            showBanner(true);
            if (isUpdate) {
                TelemetryWrapper.showBannerNew(bannerAdapter.getFirstDAOId());
            } else {
                TelemetryWrapper.showBannerUpdate(bannerAdapter.getFirstDAOId());
            }

        } catch (JSONException e) {
            LoggerWrapper.throwOrWarn(TAG, "Invalid Config: " + e.getMessage());
        }
    }

    private void writeToCache(Context context, String[] configArray) {
        try {
            final Runnable runnable = new FileUtils.WriteStringToFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), CURRENT_BANNER_CONFIG), stringArrayToString(configArray));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open cache directory when writing banner config to cache");
        }
    }

    private void deleteCache(Context context) {
        try {
            final Runnable runnable = new FileUtils.DeleteFileRunnable(new File(new FileUtils.GetCache(new WeakReference<>(context)).get(), CURRENT_BANNER_CONFIG));
            ThreadUtils.postToBackgroundThread(runnable);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            LoggerWrapper.throwOrWarn(TAG, "Failed to open cache directory when deleting banner cache");
        }
    }

    private static final String UNIT_SEPARATOR = Character.toString((char) 0x1F);
    private static final String CURRENT_BANNER_CONFIG = "CURRENT_BANNER_CONFIG";

    private String stringArrayToString(String[] stringArray) {
        return TextUtils.join(UNIT_SEPARATOR, stringArray);
    }

    private static String[] stringToStringArray(String string) {
        if (TextUtils.isEmpty(string)) {
            return new String[]{};
        }
        return string.split(UNIT_SEPARATOR);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view;
        boolean hasNewsPortal = AppConfigWrapper.hasNewsPortal();
        if (hasNewsPortal || AppConfigWrapper.hasEcommerceShoppingLink()) {
            view = inflater.inflate(R.layout.fragment_homescreen_content, container, false);

            setupContentPortalView(view);

            if (hasNewsPortal && contentPanel != null) {
                newsPresenter = new NewsPresenter(this);
                contentPanel.setNewsListListener(newsPresenter);
            }
        } else {
            view = inflater.inflate(R.layout.fragment_homescreen, container, false);
        }

        this.recyclerView = view.findViewById(R.id.main_list);

        this.btnMenu = view.findViewById(R.id.btn_menu_home);
        this.btnMenu.setOnClickListener(menuItemClickListener);

        this.btnMenu.setOnLongClickListener(v -> {
            // Long press menu always show download panel
            FragmentListener.notifyParent(HomeFragment.this, FragmentListener.TYPE.SHOW_DOWNLOAD_PANEL, null);
            TelemetryWrapper.longPressDownloadIndicator();
            return false;
        });

        sessionManager = TabsSessionProvider.getOrThrow(getActivity());
        sessionManager.register(this.observer);
        this.tabCounter = view.findViewById(R.id.btn_tab_tray);
        this.tabCounter.setOnClickListener(menuItemClickListener);
        updateTabCounter();

        this.fakeInput = view.findViewById(R.id.home_fragment_fake_input);
        this.fakeInput.setOnClickListener(v -> {
            final Activity parent = getActivity();
            if (parent instanceof FragmentListener) {
                ((FragmentListener) parent).onNotified(HomeFragment.this,
                        FragmentListener.TYPE.SHOW_URL_INPUT,
                        null);
            }
            TelemetryWrapper.showSearchBarHome();
        });
        this.banner = view.findViewById(R.id.banner);
        bannerLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        banner.setLayoutManager(bannerLayoutManager);
        SnapHelper snapHelper = new PagerSnapHelper() {

            private void sendTelemetry(int superRet, int velocityX) {
                sendSwipeTelemetry(superRet, velocityX);
                sendSwipeToIdTelemetry(superRet);
            }

            // This is kinda deprecated by sendSwipeToIdTelemetry so consider removing it in the future.
            private void sendSwipeTelemetry(int superRet, int velocityX) {
                RecyclerView.Adapter adapter = banner.getAdapter();
                if (adapter == null) {
                    return;
                }
                final int itemCount = adapter.getItemCount();
                int boundedTarget = superRet < itemCount ? superRet : itemCount - 1;
                TelemetryWrapper.swipeBannerItem(velocityX / Math.abs(velocityX), boundedTarget);
            }

            private void sendSwipeToIdTelemetry(int superRet) {
                View nextDisplayed = bannerLayoutManager.findViewByPosition(superRet);
                if (nextDisplayed == null) {
                    return;
                }
                String id = ((BannerViewHolder) banner.getChildViewHolder(nextDisplayed)).getId();
                if (id == null) {
                    return;
                }
                TelemetryWrapper.showBannerSwipe(id);
            }

            @Override
            public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX,
                                              int velocityY) {
                final int superRet = super.findTargetSnapPosition(layoutManager, velocityX, velocityY);
                sendTelemetry(superRet, velocityX);
                return superRet;
            }
        };
        snapHelper.attachToRecyclerView(banner);

        SwipeMotionLayout home_container = view.findViewById(R.id.home_container);
        home_container.setOnSwipeListener(new GestureListenerAdapter());

        if (ThemeManager.shouldShowOnboarding(view.getContext())) {
            LayoutInflater.from(view.getContext()).inflate(R.layout.fragment_homescreen_themetoy, home_container);
            themeOnboardingLayer = home_container.findViewById(R.id.fragment_homescreen_theme_onboarding);
            themeOnboardingLayer.setOnClickListener(v -> {
                if (themeOnboardingLayer != null) {
                    ThemeManager.dismissOnboarding(themeOnboardingLayer.getContext().getApplicationContext());
                    ((ViewGroup) themeOnboardingLayer.getParent()).removeView(themeOnboardingLayer);
                    themeOnboardingLayer = null;
                }
            });
        }

        homeScreenBackground = view.findViewById(R.id.home_background);

        downloadingIndicator = view.findViewById(R.id.downloading_indicator);
        downloadIndicator = view.findViewById(R.id.download_unread_indicator);

        Inject.obtainDownloadIndicatorViewModel(getActivity()).getDownloadIndicatorObservable().observe(getViewLifecycleOwner(), status -> {
            if (status == DownloadIndicatorViewModel.Status.DOWNLOADING) {
                downloadIndicator.setVisibility(View.GONE);
                downloadingIndicator.setVisibility(View.VISIBLE);
                if (!downloadingIndicator.isAnimating()) {
                    downloadingIndicator.playAnimation();
                }
            } else if (status == DownloadIndicatorViewModel.Status.UNREAD) {
                downloadingIndicator.setVisibility(View.GONE);
                downloadIndicator.setVisibility(View.VISIBLE);
                downloadIndicator.setImageResource(R.drawable.notify_download);
            } else if (status == DownloadIndicatorViewModel.Status.WARNING) {
                downloadingIndicator.setVisibility(View.GONE);
                downloadIndicator.setVisibility(View.VISIBLE);
                downloadIndicator.setImageResource(R.drawable.notify_notice);
            } else {
                downloadingIndicator.setVisibility(View.GONE);
                downloadIndicator.setVisibility(View.GONE);
            }
        });

        if (newsPresenter != null) {
            newsPresenter.setupNewsViewModel(getActivity());
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FirebaseHelper.FIREBASE_READY);
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initBanner(context);
            }
        };
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(this.receiver, intentFilter);
        }
        updateTopSitesData();
        setupBannerTimer();
        setNightModeEnabled(Settings.getInstance(getActivity()).isNightModeEnable());

        View fragmentView = getView();
        if (fragmentView != null) {
            initFeatureSurveyViewIfNecessary(fragmentView);
        }

        playContentPortalAnimation();
        if (contentPanel != null) {
            contentPanel.onResume();
        }
    }

    private void playContentPortalAnimation() {
        final Animation fadeout = AnimationUtils.loadAnimation(getActivity(), R.anim.arrow_fade_out);
        final Animation fadein = AnimationUtils.loadAnimation(getActivity(), R.anim.arrow_fade_in);
        Inject.startAnimation(arrow1, fadeout);
        Inject.startAnimation(arrow2, fadein);
    }

    private void setupBannerTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                RecyclerView.Adapter adapter = banner.getAdapter();
                if (adapter == null) {
                    cancel();
                    return;
                }
                int nextPage = (bannerLayoutManager.findFirstVisibleItemPosition() + 1) % adapter.getItemCount();
                banner.smoothScrollToPosition(nextPage);
            }
        }, SCROLL_PERIOD, SCROLL_PERIOD);
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this.receiver);
        }
        timer.cancel();
        timer = null;
        stopAnimation();
    }

    private void stopAnimation() {
        if (arrow1 != null && arrow1.getAnimation() != null) {
            arrow1.getAnimation().cancel();
        }
        if (arrow2 != null && arrow2.getAnimation() != null) {
            arrow2.getAnimation().cancel();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        doWithActivity(getActivity(), themeManager -> themeManager.subscribeThemeChange(homeScreenBackground));

        Context context = getContext();

        bannerConfigViewModel = ViewModelProviders.of(this).get(BannerConfigViewModel.class);
        bannerConfigViewModel.getConfig().observe(this, bannerObserver);
        initBanner(context);

        if (context != null) {
            this.pinSiteManager = PinSiteManagerKt.getPinSiteManager(context);
        }
    }

    @Override
    public void onDestroyView() {
        sessionManager.unregister(this.observer);
        doWithActivity(getActivity(), themeManager -> themeManager.unsubscribeThemeChange(homeScreenBackground));
        bannerConfigViewModel.getConfig().removeObserver(bannerObserver);
        super.onDestroyView();
    }

    @Override
    public void showSites(@NonNull List<Site> sites) {
        TopSideComparator topSideComparator = new TopSideComparator();
        Collections.sort(sites, topSideComparator);

        if (this.topSiteAdapter == null) {
            this.topSiteAdapter = new TopSiteAdapter(sites, clickListener, clickListener, pinSiteManager);
            this.recyclerView.setAdapter(topSiteAdapter);
        } else {
            this.recyclerView.setAdapter(topSiteAdapter);
            this.topSiteAdapter.setSites(sites);
        }
    }

    @Override
    public void applyLocale() {
        this.fakeInput.setText(R.string.urlbar_hint);
    }

    @Override
    public void appendSite(@NonNull Site site) {
        final TopSiteAdapter adapter = (TopSiteAdapter) this.recyclerView.getAdapter();
        if (adapter != null) {
            adapter.addSite(adapter.getItemCount(), site);
        }
    }

    @Override
    public void removeSite(@NonNull Site site) {
        this.topSiteAdapter.setSites(presenter.getSites());
    }

    @Override
    public void onSiteChanged(@NonNull Site site) {
        throw new NoSuchMethodError("Not implement yet");
    }

    @SuppressWarnings("unused")
    public void setPresenter(TopSitesContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void onUrlInputScreenVisible(boolean urlInputScreenVisible) {
        final int visibility = urlInputScreenVisible ? View.INVISIBLE : View.VISIBLE;
        this.fakeInput.setVisibility(visibility);
    }

    private void updateTabCounter() {
        int tabCount = sessionManager != null ? sessionManager.getTabsCount() : 0;
        if (isTabRestoredComplete()) {
            tabCounter.setCount(tabCount);
        }

        if (tabCount == 0) {
            tabCounter.setEnabled(false);
            tabCounter.setAlpha(ALPHA_TAB_COUNTER_DISABLED);

        } else {
            tabCounter.setEnabled(true);
            tabCounter.setAlpha(1f);
        }
    }

    private boolean isTabRestoredComplete() {
        return (getActivity() instanceof MainActivity)
                && ((MainActivity) getActivity()).isTabRestoredComplete();
    }

    private class SiteItemClickListener implements View.OnClickListener, View.OnLongClickListener {

        @Override
        public void onClick(View v) {
            final Site site = (Site) v.getTag();
            final Activity parent = getActivity();
            if ((site != null) && (parent instanceof FragmentListener)) {
                ScreenNavigator.get(v.getContext()).showBrowserScreen(site.getUrl(), true, false);
                ViewParent viewParent = v.getParent();
                if (viewParent instanceof ViewGroup) {
                    int index = ((ViewGroup) v.getParent()).indexOfChild(v);
                    TelemetryWrapper.clickTopSiteOn(index, site.isDefault() ? site.getTitle() : "");
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            final Site site = (Site) v.getTag();

            if (site == null) {
                return false;
            }
            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v, Gravity.CLIP_HORIZONTAL);
            popupMenu.getMenuInflater().inflate(R.menu.menu_top_site_item, popupMenu.getMenu());

            MenuItem pinItem = popupMenu.getMenu().findItem(R.id.pin);
            if (pinItem != null) {
                pinItem.setVisible(pinSiteManager.isEnabled() && !pinSiteManager.isPinned(site));
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.pin:
                        presenter.pinSite(site, HomeFragment.this::refreshTopSites);
                        break;
                    case R.id.remove:
                        if (site.getId() < 0) {
                            presenter.removeSite(site);
                            removeDefaultSites(site);
                            TopSitesUtils.saveDefaultSites(getContext(), HomeFragment.this.orginalDefaultSites);
                            refreshTopSites();
                            TelemetryWrapper.removeTopSite(true);
                        } else {
                            site.setViewCount(1);
                            BrowsingHistoryManager.getInstance().updateLastEntry(site, mTopSiteUpdateListener);
                            TelemetryWrapper.removeTopSite(false);
                        }
                        pinSiteManager.unpinned(site);
                        break;
                    default:
                        throw new IllegalStateException("Unhandled menu item");
                }

                return true;
            });
            popupMenu.show();

            return true;
        }
    }

    private QueryHandler.AsyncQueryListener mTopSitesQueryListener = sites -> {
        List<Site> querySites = new ArrayList<>();
        for (Object site : sites) {
            if (site instanceof Site) {
                querySites.add((Site) site);
            }
        }

        constructTopSiteList(querySites);
    };

    private QueryHandler.AsyncUpdateListener mTopSiteUpdateListener = result -> refreshTopSites();

    private void refreshTopSites() {
        BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT,
                TOP_SITES_QUERY_MIN_VIEW_COUNT,
                mTopSitesQueryListener);
    }

    private void constructTopSiteList(List<Site> historySites) {
        //if history data are equal to the default data, merge them
        initDefaultSitesFromJSONArray(this.orginalDefaultSites);
        List<Site> topSites = new ArrayList<>(this.presenter.getSites());

        mergeHistorySiteToTopSites(historySites, topSites);
        mergePinSiteToTopSites(pinSiteManager.getPinSites(), topSites);

        TopSideComparator topSideComparator = new TopSideComparator();
        Collections.sort(topSites, topSideComparator);

        if (topSites.size() > TOP_SITES_QUERY_LIMIT) {
            List<Site> removeSites = topSites.subList(TOP_SITES_QUERY_LIMIT, topSites.size());
            removeDefaultSites(removeSites);

            topSites = topSites.subList(0, TOP_SITES_QUERY_LIMIT);
        }

        this.presenter.setSites(topSites);
        this.presenter.populateSites();
    }

    private void mergeHistorySiteToTopSites(List<Site> historySites, List<Site> topSites) {
        for (Site topSite : topSites) {
            removeDuplicatedSites(historySites,
                    topSite,
                    site -> topSite.setViewCount(topSite.getViewCount() + site.getViewCount()));
        }
        topSites.addAll(historySites);
    }

    private void mergePinSiteToTopSites(List<Site> pinSites, List<Site> topSites) {
        for (Site pinSite : pinSites) {
            removeDuplicatedSites(topSites, pinSite, site -> { /* Do nothing */ });
        }
        topSites.addAll(pinSites);
    }

    private void initDefaultSites() {
        // use different implementation to provide default top sites.
        String obj_sites = Inject.getDefaultTopSites(getContext());

        //if no default sites data in SharedPreferences, load data from assets.
        if (obj_sites == null) {
            this.orginalDefaultSites = TopSitesUtils.getDefaultSitesJsonArrayFromAssets(getContext());
        } else {
            try {
                this.orginalDefaultSites = new JSONArray(obj_sites);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }

        initDefaultSitesFromJSONArray(this.orginalDefaultSites);
    }

    private void initDefaultSitesFromJSONArray(JSONArray jsonDefault) {
        List<Site> defaultSites = TopSitesUtils.paresJsonToList(getContext(), jsonDefault);
        this.presenter.setSites(defaultSites);
    }

    private void removeDefaultSites(List<Site> removeSites) {
        boolean isRemove = false;
        for (int i = 0; i < removeSites.size(); i++) {
            Site rSite = removeSites.get(i);
            if (rSite.getId() < 0) {
                removeDefaultSites(rSite);
                isRemove = true;
            }
        }

        if (isRemove) {
            TopSitesUtils.saveDefaultSites(getContext(), this.orginalDefaultSites);
        }
    }

    private void removeDefaultSites(Site removeSite) {
        try {
            if (this.orginalDefaultSites != null) {
                for (int i = 0; i < this.orginalDefaultSites.length(); i++) {
                    long id = ((JSONObject) this.orginalDefaultSites.get(i)).getLong("id");

                    if (id == removeSite.getId()) {
                        this.orginalDefaultSites.remove(i);
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initFeatureSurveyViewIfNecessary(final View view) {
        final RemoteConfigConstants.SURVEY featureSurvey = RemoteConfigConstants.SURVEY.Companion.parseLong(AppConfigWrapper.getFeatureSurvey());
        final ImageView imgSurvey = view.findViewById(R.id.home_wifi_vpn_survey);
        final Settings.EventHistory eventHistory = Settings.getInstance(getContext()).getEventHistory();
        if (featureSurvey == RemoteConfigConstants.SURVEY.WIFI_FINDING && !eventHistory.contains(Settings.Event.FeatureSurveyWifiFinding)) {
            imgSurvey.setImageResource(R.drawable.find_wifi);
            imgSurvey.setVisibility(View.VISIBLE);
            if (getContext() != null) {
                imgSurvey.setOnClickListener(new FeatureSurveyViewHelper(getContext(), featureSurvey));
            }
        } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN && !eventHistory.contains(Settings.Event.FeatureSurveyVpn)) {
            imgSurvey.setImageResource(R.drawable.vpn);
            imgSurvey.setVisibility(View.VISIBLE);
            if (getContext() != null) {
                imgSurvey.setOnClickListener(new FeatureSurveyViewHelper(getContext(), featureSurvey));
            }
        } else if (featureSurvey == RemoteConfigConstants.SURVEY.VPN_RECOMMENDER && !eventHistory.contains(Settings.Event.VpnRecommenderIgnore)) {
            PackageInfo packageInfo = null;
            final String packageName = AppConfigWrapper.getVpnRecommenderPackage();
            try {
                Activity activity = getActivity();
                if (activity != null) {
                    packageInfo = activity.getPackageManager().getPackageInfo(packageName, 0);
                }
            } catch (PackageManager.NameNotFoundException ex) {
                ex.printStackTrace();
            }
            if (packageInfo != null) {
                eventHistory.add(Settings.Event.VpnAppWasDownloaded);
                // Show vpn recommender, click vpn will launch vpn app
                imgSurvey.setImageResource(R.drawable.vpn);
                imgSurvey.setVisibility(View.VISIBLE);
                imgSurvey.setOnClickListener(v -> {
                    Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(packageName);
                    startActivity(intent);
                    TelemetryWrapper.clickVpnRecommender(true);
                });
                TelemetryWrapper.showVpnRecommender(true);
            } else {
                // No vpn app is installed
                if (eventHistory.contains(Settings.Event.VpnAppWasDownloaded)) {
                    // Vpn app was downloaded before, hide vpn recommender
                    imgSurvey.setVisibility(View.GONE);
                } else {
                    // Vpn app wasn't downloaded before, show vpn recommender hint
                    if (getContext() != null) {
                        imgSurvey.setOnClickListener(new FeatureSurveyViewHelper(getContext(), featureSurvey));
                        imgSurvey.setVisibility(View.VISIBLE);
                    }
                    TelemetryWrapper.showVpnRecommender(false);
                }
            }
        } else {
            imgSurvey.setVisibility(View.GONE);
        }
    }

    public void updateTopSitesData() {
        initDefaultSites();
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPreferences.contains(TOP_SITES_V2_PREF)) {
            refreshTopSites();
        } else {
            new Thread(new MigrateHistoryRunnable(uiHandler, getContext())).start();
        }
    }

    // TODO: Reduce complexity here, a possible solution is to use the HashMap for site list
    private void removeDuplicatedSites(List<Site> sites, Site site, OnRemovedListener listener) {
        final Iterator<Site> siteIterator = sites.iterator();
        while (siteIterator.hasNext()) {
            Site nextSite = siteIterator.next();
            if (UrlUtils.urlsMatchExceptForTrailingSlash(nextSite.getUrl(), site.getUrl())) {
                siteIterator.remove();
                listener.onRemoved(nextSite);
            }
        }
    }

    private interface OnRemovedListener {
        void onRemoved(Site site);
    }

    private static void parseCursorToSite(Cursor cursor, List<String> urls, List<byte[]> icons) {
        String url = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.URL));
        byte[] icon = cursor.getBlob(cursor.getColumnIndex(HistoryContract.BrowsingHistory.FAV_ICON));
        urls.add(url);
        icons.add(icon);
    }

    private static class MigrateHistoryRunnable implements Runnable {

        private WeakReference<Handler> handlerWeakReference;
        private WeakReference<Context> contextWeakReference;

        MigrateHistoryRunnable(Handler handler, Context context) {
            handlerWeakReference = new WeakReference<>(handler);
            contextWeakReference = new WeakReference<>(context);
        }

        @Override
        public void run() {
            Context context = contextWeakReference.get();
            if (context == null) {
                return;
            }

            final SupportSQLiteOpenHelper helper = HistoryDatabase.getInstance(context).getOpenHelper();
            final SupportSQLiteDatabase db = helper.getWritableDatabase();
            // We can't differentiate if this is a new install or upgrade given the db version will
            // already become the latest version here. We create a temp table if no migration is
            // needed and later delete it to prevent crashing.
            db.execSQL(HistoryDatabase.CREATE_LEGACY_IF_NOT_EXIST);
            final SupportSQLiteQueryBuilder builder = SupportSQLiteQueryBuilder.builder(HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY);
            final String[] columns = {HistoryContract.BrowsingHistory._ID, HistoryContract.BrowsingHistory.URL, HistoryContract.BrowsingHistory.FAV_ICON};
            builder.columns(columns);
            final SupportSQLiteQuery query = builder.create();
            final File faviconFolder = FileUtils.getFaviconFolder(context);
            final List<String> urls = new ArrayList<>();
            final List<byte[]> icons = new ArrayList<>();
            try (Cursor cursor = db.query(query)) {
                if (cursor.moveToFirst()) {
                    parseCursorToSite(cursor, urls, icons);
                }
                while (cursor.moveToNext()) {
                    parseCursorToSite(cursor, urls, icons);
                }
            }
            Handler handler = handlerWeakReference.get();
            if (handler == null) {
                return;
            }
            if (icons.size() == 0) {
                scheduleRefresh(handler);
            } else {
                // Refresh is still scheduled implicitly in SaveBitmapsTask
                new FavIconUtils.SaveBitmapsTask(faviconFolder, urls, icons, new UpdateHistoryWrapper(urls, handlerWeakReference),
                        Bitmap.CompressFormat.PNG, DimenUtils.PNG_QUALITY_DONT_CARE).execute();
            }
            db.execSQL("DROP TABLE " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(TOP_SITES_V2_PREF, true).apply();
        }
    }

    private View.OnClickListener menuItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Activity parent = getActivity();
            if (parent instanceof FragmentListener) {
                dispatchOnClick(v, (FragmentListener) parent);
            }
        }

        private void dispatchOnClick(View view, FragmentListener listener) {
            switch (view.getId()) {
                case R.id.btn_menu_home:
                    listener.onNotified(HomeFragment.this, FragmentListener.TYPE.SHOW_MENU,
                            null);
                    TelemetryWrapper.showMenuHome();
                    break;

                case R.id.btn_tab_tray:
                    listener.onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_TAB_TRAY,
                            null);
                    TelemetryWrapper.showTabTrayHome();
                    break;

                default:
                    break;
            }
        }
    };

    private class SessionManagerObserver implements SessionManager.Observer {

        @Override
        public void onFocusChanged(@Nullable Session tab, @NotNull SessionManager.Factor factor) {
            // do nothing
        }

        @Override
        public void onSessionAdded(@NonNull Session tab, @Nullable Bundle arguments) {
            // do nothing
        }

        @Override
        public void onSessionCountChanged(int count) {
            updateTabCounter();
        }

        @Override
        public void updateFailingUrl(@org.jetbrains.annotations.Nullable String url, boolean updateFromError) {
            // do nothing
        }

        @Override
        public boolean handleExternalUrl(@org.jetbrains.annotations.Nullable String url) {
            // do nothing
            return false;
        }

        @Override
        public boolean onShowFileChooser(@NotNull TabViewEngineSession es, @org.jetbrains.annotations.Nullable ValueCallback<Uri[]> filePathCallback, @org.jetbrains.annotations.Nullable WebChromeClient.FileChooserParams fileChooserParams) {
            // do nothing
            return false;
        }

        @Override
        public void onHttpAuthRequest(@NotNull TabViewClient.HttpAuthCallback callback, @Nullable String host, @Nullable String realm) {
            // do nothing
        }
    }

    private interface DoWithThemeManager {
        void doIt(ThemeManager themeManager);
    }

    private static void doWithActivity(Activity activity, DoWithThemeManager doWithThemeManager) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (activity instanceof ThemeManager.ThemeHost) {
            ThemeManager.ThemeHost themeHost = (ThemeManager.ThemeHost) activity;
            doWithThemeManager.doIt(themeHost.getThemeManager());
        }
    }


    private class GestureListenerAdapter implements OnSwipeListener {

        @Override
        public void onSwipeUp() {
            if (contentPanel != null) {
                showContentPortal();
            } else {
                btnMenu.performClick();
            }
        }

        @Override
        public void onSwipeDown() {
            if (contentPanel == null) {
                fakeInput.performClick();
            }
        }

        @Override
        public void onLongPress() {
            if (Settings.getInstance(getActivity()).isNightModeEnable()) {
                // Not allowed long press to reset theme when night mode is on
                return;
            }
            doWithActivity(getActivity(), (themeManager) -> {
                themeManager.resetDefaultTheme();
                TelemetryWrapper.resetThemeToDefault();
            });
        }

        @Override
        public boolean onDoubleTap() {
            if (Settings.getInstance(getActivity()).isNightModeEnable()) {
                // Not allowed double tap to switch theme when night mode is on
                return true;
            }
            doWithActivity(getActivity(), (themeManager) -> {
                ThemeManager.ThemeSet themeSet = themeManager.toggleNextTheme();
                TelemetryWrapper.changeThemeTo(themeSet.name());
            });
            return true;
        }

    }

    private static class UpdateHistoryWrapper implements FavIconUtils.Consumer<List<String>> {

        private List<String> urls;
        private WeakReference<Handler> handlerWeakReference;

        private UpdateHistoryWrapper(List<String> urls, WeakReference<Handler> handlerWeakReference) {
            this.urls = urls;
            this.handlerWeakReference = handlerWeakReference;
        }

        @Override
        public void accept(List<String> fileUris) {
            QueryHandler.AsyncUpdateListener listener = result -> {
                Handler handler = handlerWeakReference.get();
                if (handler == null) {
                    return;
                }
                scheduleRefresh(handler);
            };
            for (int i = 0; i < fileUris.size(); i++) {
                if (i == fileUris.size() - 1) {
                    BrowsingHistoryManager.updateHistory(null, urls.get(i), fileUris.get(i), listener);
                } else {
                    BrowsingHistoryManager.updateHistory(null, urls.get(i), fileUris.get(i));
                }
            }
        }
    }

    private static void scheduleRefresh(Handler handler) {
        Message message = handler.obtainMessage(MSG_ID_REFRESH);
        handler.dispatchMessage(message);
    }

    public void setNightModeEnabled(boolean enable) {
        fakeInput.setNightMode(enable);
        btnMenu.setNightMode(enable);
        tabCounter.setNightMode(enable);
        homeScreenBackground.setNightMode(enable);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            final ThemedTextView item = recyclerView.getChildAt(i).findViewById(R.id.text);
            item.setNightMode(enable);
        }
        Activity activity = getActivity();
        if (activity != null) {
            ViewUtils.updateStatusBarStyle(!enable, activity.getWindow());
        }
    }

    private void showContentPortal() {
        if (contentPanel != null) {
            contentPanel.show(true);
            if (AppConfigWrapper.hasEcommerceShoppingLink()) {
                TelemetryWrapper.openLifeFeedEc();
            } else {
                TelemetryWrapper.openLifeFeedNews();
            }
        }
    }

    private void setupContentPortalView(View view) {
        this.arrow1 = view.findViewById(R.id.arrow1);
        this.arrow2 = view.findViewById(R.id.arrow2);
        this.contentPanel = view.findViewById(R.id.content_panel);
        final View arrowContainer = view.findViewById(R.id.arrow_container);
        if (arrowContainer != null) {
            arrowContainer.setOnTouchListener(new SwipeMotionDetector(getContext(), new OnSwipeListener() {

                @Override
                public boolean onSingleTapConfirmed() {
                    showContentPortal();
                    return true;
                }
                @Override
                public  void onSwipeUp() {
                    showContentPortal();
                }

            }));
        }
    }
}
