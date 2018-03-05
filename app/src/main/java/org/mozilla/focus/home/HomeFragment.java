/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.provider.QueryHandler;
import org.mozilla.focus.tabs.TabCounter;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.OnSwipeListener;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.SwipeMotionLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HomeFragment extends LocaleAwareFragment implements TopSitesContract.View {

    public static final String FRAGMENT_TAG = "homescreen";
    public static final String TOPSITES_PREF = "topsites_pref";
    public static final int REFRESH_REQUEST_CODE = 911;
    public static final int TOP_SITES_QUERY_LIMIT = 8;
    public static final int TOP_SITES_QUERY_MIN_VIEW_COUNT = 6;

    private static final float ALPHA_TAB_COUNTER_DISABLED = 0.3f;

    private TopSitesContract.Presenter presenter;
    private RecyclerView recyclerView;
    private View btnMenu;
    private TabCounter tabCounter;
    private TextView fakeInput;
    private SiteItemClickListener clickListener = new SiteItemClickListener();
    private TopSiteAdapter topSiteAdapter;
    private JSONArray orginalDefaultSites = null;

    public static HomeFragment create() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.presenter = new TopSitesPresenter();
        this.presenter.setView(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_homescreen, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.main_list);

        this.btnMenu = view.findViewById(R.id.btn_menu);
        this.btnMenu.setOnClickListener(menuItemClickListener);

        this.tabCounter = view.findViewById(R.id.btn_tab_tray);
        this.tabCounter.setOnClickListener(menuItemClickListener);
        updateTabCounter();

        this.fakeInput = (TextView) view.findViewById(R.id.home_fragment_fake_input);
        this.fakeInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity parent = getActivity();
                if (parent instanceof FragmentListener) {
                    ((FragmentListener) parent).onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_URL_INPUT,
                            null);
                }
                TelemetryWrapper.showSearchBarHome();
            }
        });

        SwipeMotionLayout home_container = (SwipeMotionLayout) view.findViewById(R.id.home_container);
        home_container.setOnSwipeListener(new OnSwipeListener.OnSwipeListenerAdapter() {
            @Override
            public void onSwipeUp() {
                btnMenu.performClick();
            }

            @Override
            public void onSwipeDown() {
                fakeInput.performClick();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTopSitesData();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STARTED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STOPPED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void showSites(@NonNull List<Site> sites) {
        if (this.topSiteAdapter == null) {
            this.topSiteAdapter = new TopSiteAdapter(sites, clickListener, clickListener);
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
        adapter.addSite(adapter.getItemCount(), site);
    }

    @Override
    public void removeSite(@NonNull Site site) {
        this.topSiteAdapter.setSites(presenter.getSites());
    }

    @Override
    public void onSiteChanged(@NonNull Site site) {
        throw new NoSuchMethodError("Not implement yet");
    }

    public void setPresenter(TopSitesContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void toggleFakeUrlInput(boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        this.fakeInput.setVisibility(visibility);
    }

    private void updateTabCounter() {
        TabsSession tabsSession = TabsSessionProvider.getOrNull(getActivity());
        int tabCount = tabsSession != null ? tabsSession.getTabsCount() : 0;
        tabCounter.setCount(tabCount);

        if (tabCount == 0) {
            tabCounter.setEnabled(false);
            tabCounter.setAlpha(ALPHA_TAB_COUNTER_DISABLED);

        } else {
            tabCounter.setEnabled(true);
            tabCounter.setAlpha(1f);
        }
    }

    private class SiteItemClickListener implements View.OnClickListener, View.OnLongClickListener {

        @Override
        public void onClick(View v) {
            final Site site = (Site) v.getTag();
            final Activity parent = getActivity();
            if ((site != null) && (parent instanceof FragmentListener)) {
                ((FragmentListener) parent).onNotified(HomeFragment.this,
                        FragmentListener.TYPE.OPEN_URL,
                        site.getUrl());
                ViewParent viewParent = v.getParent();
                if (viewParent instanceof ViewGroup) {
                    int index = ((ViewGroup) v.getParent()).indexOfChild(v);
                    TelemetryWrapper.clickTopSiteOn(index);
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
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.remove:
                            if (site.getId() < 0) {
                                HomeFragment.this.presenter.removeSite(site);
                                HomeFragment.this.removeDefaultSites(site);
                                TopSitesUtils.saveDefaultSites(getContext(), HomeFragment.this.orginalDefaultSites);
                                BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
                                TelemetryWrapper.removeTopSite(true);
                            } else {
                                site.setViewCount(1);
                                BrowsingHistoryManager.getInstance().updateLastEntry(site, mTopSiteUpdateListener);
                                TelemetryWrapper.removeTopSite(false);
                            }
                            break;
                        default:
                            throw new IllegalStateException("Unhandled menu item");
                    }

                    return true;
                }
            });
            popupMenu.show();

            return true;
        }
    }

    private QueryHandler.AsyncQueryListener mTopSitesQueryListener = new QueryHandler.AsyncQueryListener() {
        @Override
        public void onQueryComplete(List sites) {
            List<Site> querySites = new ArrayList<>();
            for (Object site : sites) {
                if (site instanceof Site) {
                    querySites.add((Site) site);
                }
            }

            HomeFragment.this.mergeQueryAndDefaultSites(querySites);
        }
    };

    private QueryHandler.AsyncUpdateListener mTopSiteUpdateListener = new QueryHandler.AsyncUpdateListener() {
        @Override
        public void onUpdateComplete(int result) {
            BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
        }
    };

    private void mergeQueryAndDefaultSites(List<Site> querySites) {
        //if query data are equal to the default data, merge them
        initDefaultSitesFromJSONArray(this.orginalDefaultSites);
        List<Site> topSites = new ArrayList<>(this.presenter.getSites());
        for (Site topSite : topSites) {
            Iterator<Site> querySitesIterator = querySites.iterator();
            while (querySitesIterator.hasNext()) {
                Site temp = querySitesIterator.next();
                if (UrlUtils.urlsMatchExceptForTrailingSlash(topSite.getUrl(), temp.getUrl())) {
                    topSite.setViewCount(topSite.getViewCount() + temp.getViewCount());
                    querySitesIterator.remove();
                }
            }
        }

        topSites.addAll(querySites);
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

    private void initDefaultSites() {
        String obj_sites = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(HomeFragment.TOPSITES_PREF, null);

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

    public void updateTopSitesData() {
        initDefaultSites();
        BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REFRESH_REQUEST_CODE) {
            updateTopSitesData();
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
                case R.id.btn_menu:
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
}
