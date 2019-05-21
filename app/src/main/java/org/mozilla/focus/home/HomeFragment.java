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
import android.os.StrictMode;
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
import android.text.method.LinkMovementMethod;
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
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.banner.BannerAdapter;
import org.mozilla.banner.BannerConfigViewModel;
import org.mozilla.banner.BannerViewHolder;
import org.mozilla.banner.OnClickListener;
import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.provider.HistoryContract;
import org.mozilla.focus.provider.HistoryDatabaseHelper;
import org.mozilla.focus.provider.QueryHandler;
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
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.SwipeMotionLayout;
import org.mozilla.icon.FavIconUtils;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.BottomBarViewModel;
import org.mozilla.rocket.chrome.ChromeViewModel;
import org.mozilla.rocket.content.LifeFeedOnboarding;
import org.mozilla.rocket.content.portal.ContentFeature;
import org.mozilla.rocket.content.portal.ContentPortalView;
import org.mozilla.rocket.content.view.BottomBar;
import org.mozilla.rocket.home.pinsite.PinSiteManager;
import org.mozilla.rocket.home.pinsite.PinSiteManagerKt;
import org.mozilla.rocket.nightmode.themed.ThemedTextView;
import org.mozilla.rocket.persistance.History.HistoryDatabase;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabViewClient;
import org.mozilla.rocket.tabs.TabViewEngineSession;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.theme.ThemeManager;
import org.mozilla.strictmodeviolator.StrictModeViolation;
import org.mozilla.urlutils.UrlUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_DEFAULT;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_DOWNLOADING;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_UNREAD;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_WARNING;

public class HomeFragment extends LocaleAwareFragment implements TopSitesContract.View, TopSitesContract.Model,
        ScreenNavigator.HomeScreen, BannerHelper.HomeBannerHelperListener {
    private static final String TAG = "HomeFragment";

    public static final String TOPSITES_PREF = "topsites_pref";
    public static final String TOP_SITES_V2_PREF = "top_sites_v2_complete";
    public static final int TOP_SITES_QUERY_LIMIT = 8;
    public static final int TOP_SITES_QUERY_MIN_VIEW_COUNT = 6;
    private static final int MSG_ID_REFRESH = 8269;

    public static final String BANNER_MANIFEST_DEFAULT = "";

    private TopSitesContract.Presenter presenter;
    private RecyclerView recyclerView;
    @Nullable private ImageButton arrow1;
    @Nullable private ImageButton arrow2;
    @Nullable private ContentPortalView contentPanel;

    @NonNull private BannerHelper bannerHelper = new BannerHelper();
    @NonNull private ContentFeature contentFeature = new ContentFeature();

    private View lifeFeedOnboardingLayer;
    private ThemedTextView fakeInput;
    private HomeScreenBackground homeScreenBackground;
    private SiteItemClickListener clickListener = new SiteItemClickListener();
    private TopSiteAdapter topSiteAdapter;
    private JSONArray orginalDefaultSites = null;
    private SessionManager sessionManager;
    private final SessionManagerObserver observer = new SessionManagerObserver();
    private RecyclerView homeBanner;
    private LinearLayoutManager bannerLayoutManager;
    private BroadcastReceiver receiver;
    private Timer timer;
    private static final int SCROLL_PERIOD = 10000;
    private ChromeViewModel chromeViewModel;
    private BannerConfigViewModel bannerConfigViewModel;
    final Observer<String[]> homeBannerObserver = bannerHelper::setUpHomeBannerFromConfig;
    private BottomBarItemAdapter bottomBarItemAdapter;
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
        bannerHelper.setListener(this);
        chromeViewModel = Inject.obtainChromeViewModel(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        showCurrentBannerTelemetry();
        TelemetryWrapper.showHome();
    }

    private void showCurrentBannerTelemetry() {
        if (homeBanner.getVisibility() != View.VISIBLE || bannerLayoutManager == null) {
            return;
        }
        // Since we're using SnapHelper, the only shown child would be at position 0
        final int displayedChildPosition = 0;
        View displayedView = homeBanner.getChildAt(displayedChildPosition);
        if (displayedView == null) {
            return;
        }
        String id = ((BannerViewHolder) homeBanner.getChildViewHolder(displayedView)).getId();
        if (id == null) {
            return;
        }
        TelemetryWrapper.showBannerReturn(id);
    }

    private void showView(@NonNull View v, boolean enabled) {
        if (enabled) {
            v.setVisibility(View.VISIBLE);
        } else {
            v.setVisibility(View.GONE);
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
    public void hideHomeBannerProcedure(Void v) {
        homeBanner.setAdapter(null);
        showView(homeBanner, false);
    }

    @Override
    public void showHomeBannerProcedure(BannerAdapter b) {
        homeBanner.setAdapter(b);
        showView(homeBanner, true);
    }

    @Override
    public OnClickListener onBannerClickListener() {
        return arg -> FragmentListener.notifyParent(this, FragmentListener.TYPE.OPEN_URL_IN_NEW_TAB, arg);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view;
        if (contentFeature.hasContentPortal()) {
            view = inflater.inflate(R.layout.fragment_homescreen_content, container, false);

            setupContentPortalView(view);
        } else {
            view = inflater.inflate(R.layout.fragment_homescreen, container, false);
        }

        this.recyclerView = view.findViewById(R.id.main_list);

        setupBottomBar(view);

        sessionManager = TabsSessionProvider.getOrThrow(getActivity());
        sessionManager.register(this.observer);

        this.fakeInput = view.findViewById(R.id.home_fragment_fake_input);
        this.fakeInput.setOnClickListener(v -> {
            chromeViewModel.getShowUrlInput().call();
            TelemetryWrapper.showSearchBarHome();
        });
        this.homeBanner = view.findViewById(R.id.banner);
        bannerLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        homeBanner.setLayoutManager(bannerLayoutManager);
        SnapHelper snapHelper = new PagerSnapHelper() {

            private void sendTelemetry(int superRet, int velocityX) {
                sendSwipeTelemetry(superRet, velocityX);
                sendSwipeToIdTelemetry(superRet);
            }

            // This is kinda deprecated by sendSwipeToIdTelemetry so consider removing it in the future.
            private void sendSwipeTelemetry(int superRet, int velocityX) {
                RecyclerView.Adapter adapter = homeBanner.getAdapter();
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
                String id = ((BannerViewHolder) homeBanner.getChildViewHolder(nextDisplayed)).getId();
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
        snapHelper.attachToRecyclerView(homeBanner);

        SwipeMotionLayout home_container = view.findViewById(R.id.home_container);
        home_container.setOnSwipeListener(new GestureListenerAdapter());

        if (LifeFeedOnboarding.shouldShow(getContext())) {
            LifeFeedOnboarding.hasShown(getContext());

            LayoutInflater.from(view.getContext()).inflate(R.layout.fragment_homescreen_life_feed_onboarding, home_container);
            final TextView contentTextView = home_container.findViewById(R.id.life_feed_onboarding_content);
            contentTextView.setText(LifeFeedOnboarding.getContentText(getContext()));
            contentTextView.setMovementMethod(LinkMovementMethod.getInstance());

            lifeFeedOnboardingLayer = home_container.findViewById(R.id.life_feed_onboarding_root);
            lifeFeedOnboardingLayer.setOnClickListener(v -> {
                dismissLifeFeedOnboarding();
            });
        }

        homeScreenBackground = view.findViewById(R.id.home_background);

        return view;
    }

    private void dismissLifeFeedOnboarding() {
        if (lifeFeedOnboardingLayer != null) {
            ((ViewGroup) lifeFeedOnboardingLayer.getParent()).removeView(lifeFeedOnboardingLayer);
            lifeFeedOnboardingLayer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FirebaseHelper.FIREBASE_READY);
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                bannerHelper.initHomeBanner(context, bannerConfigViewModel.getHomeConfig());
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
                RecyclerView.Adapter adapter = homeBanner.getAdapter();
                if (adapter == null) {
                    cancel();
                    return;
                }
                int nextPage = (bannerLayoutManager.findFirstVisibleItemPosition() + 1) % adapter.getItemCount();
                homeBanner.smoothScrollToPosition(nextPage);
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

        dismissLifeFeedOnboarding();
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

        bannerConfigViewModel = ViewModelProviders.of(getActivity()).get(BannerConfigViewModel.class);
        bannerConfigViewModel.getHomeConfig().observe(this, homeBannerObserver);
        bannerHelper.initHomeBanner(context, bannerConfigViewModel.getHomeConfig());

        if (context != null) {
            StrictModeViolation.tempGrant(StrictMode.ThreadPolicy.Builder::permitDiskReads, () -> {
                this.pinSiteManager = PinSiteManagerKt.getPinSiteManager(context);
                return null;
            });
        }
    }

    @Override
    public void onDestroyView() {
        sessionManager.unregister(this.observer);
        doWithActivity(getActivity(), themeManager -> themeManager.unsubscribeThemeChange(homeScreenBackground));
        bannerConfigViewModel.getHomeConfig().removeObserver(homeBannerObserver);
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

    private void setupBottomBar(View rootView) {
        BottomBar bottomBar = rootView.findViewById(R.id.bottom_bar);
        // Hard code to only show the first and last items in home page for now
        bottomBar.setItemVisibility(1, View.INVISIBLE);
        bottomBar.setItemVisibility(2, View.INVISIBLE);
        bottomBar.setItemVisibility(3, View.INVISIBLE);
        bottomBar.setOnItemClickListener((type, position) -> {
            switch (type) {
                case BottomBarItemAdapter.TYPE_TAB_COUNTER:
                    chromeViewModel.getShowTabTray().call();
                    TelemetryWrapper.showTabTrayHome();
                    break;
                case BottomBarItemAdapter.TYPE_MENU:
                    chromeViewModel.getShowMenu().call();
                    TelemetryWrapper.showMenuHome();
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled menu item in BrowserFragment, type: " + type);
            }
        });
        bottomBar.setOnItemLongClickListener((type, position) -> {
            if (type == BottomBarItemAdapter.TYPE_MENU) {
                chromeViewModel.getShowDownloadPanel().call();
                TelemetryWrapper.longPressDownloadIndicator();
                return true;
            }
            return false;
        });
        bottomBarItemAdapter = new BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.DARK.INSTANCE);
        BottomBarViewModel bottomBarViewModel = Inject.obtainBottomBarViewModel(getActivity());
        bottomBarViewModel.getItems().observe(this, bottomBarItemAdapter::setItems);

        chromeViewModel.getTabCount().observe(this, changedEvent -> {
            bottomBarItemAdapter.setTabCount(changedEvent.getCount(), changedEvent.getWithAnimation());
        });
        chromeViewModel.isNightMode().observe(this, bottomBarItemAdapter::setNightMode);

        setupDownloadIndicator();
    }

    private void setupDownloadIndicator() {
        Inject.obtainDownloadIndicatorViewModel(getActivity()).getDownloadIndicatorObservable().observe(getViewLifecycleOwner(), status -> {
            switch (status) {
                case DOWNLOADING:
                    bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_DOWNLOADING);
                    break;
                case UNREAD:
                    bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_UNREAD);
                    break;
                case WARNING:
                    bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_WARNING);
                    break;
                case DEFAULT:
                    bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_DEFAULT);
                    break;
            }
        });
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
            int tabCount = sessionManager != null ? sessionManager.getTabsCount() : 0;
            if (isTabRestoredComplete()) {
                chromeViewModel.onTabCountChanged(tabCount);
            }
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
                chromeViewModel.getShowMenu().call();
                TelemetryWrapper.showMenuHome();
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

            if (contentFeature.hasCoupon()) {
                // default tab for ec is coupon. so we send the telemetry as coupon
                TelemetryWrapper.openLifeFeedPromo(TelemetryWrapper.Extra_Value.ARROW);
            } else if (contentFeature.hasNews()) {
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
