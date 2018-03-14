/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.web.DownloadCallback;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {TabsSessionTest.ShadowTab.class})
public class TabsSessionTest {

    TabsSession session;
    final List<TabModel> models = new ArrayList<>();

    final String[] urls = new String[]{
            "https://mozilla.org",
            "https://mozilla.com",
            "https://wikipedia.org",
            "https://en.wikipedia.org/wiki/Taiwan"
    };

    @Before
    public void setUp() {
        final Activity activity = Mockito.mock(MainActivity.class);
        session = new TabsSession(activity);

        for (int i = 0; i < urls.length; i++) {
            // use url as id for convenience
            final TabModel model = new TabModel(urls[i], "", urls[i], urls[i]);
            models.add(model);
        }
    }

    @After
    public void cleanUp() {
    }

    @Test
    public void testExistence() {
        Assert.assertNotNull(session);
    }

    @Test
    public void testAddTab1() {
        /* no parent id, tab should be append to tail */

        final String tabId0 = session.addTab(null, "url0", false, false);
        final String tabId1 = session.addTab(null, "url1", false, false);
        final String tabId2 = session.addTab(null, "url2", false, false);
        Assert.assertEquals(3, session.getTabs().size());
        Assert.assertEquals(session.getTabs().get(0).getId(), tabId0);
        Assert.assertEquals(session.getTabs().get(1).getId(), tabId1);
        Assert.assertEquals(session.getTabs().get(2).getId(), tabId2);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(1).getParentId()));
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(2).getParentId()));
    }

    @Test
    public void testAddTab2() {
        /* Tab 2 use Tab0 as parent */

        String tabId0 = session.addTab(null, "url0", false, false);
        Assert.assertEquals(1, session.getTabs().size());

        // this tab should not have parent
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));

        // no parent, should be append to tail
        String tabId1 = session.addTab(null, "url1", false, false);

        // tab0 is parent, order should be: tabId0 -> tabId2 -> tabId1
        String tabId2 = session.addTab(tabId0, "url2", false, false);

        Assert.assertEquals(session.getTabs().get(0).getId(), tabId0);
        Assert.assertEquals(session.getTabs().get(1).getId(), tabId2);
        Assert.assertEquals(session.getTabs().get(2).getId(), tabId1);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));
        Assert.assertEquals(session.getTabs().get(1).getParentId(), tabId0);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(2).getParentId()));
    }

    @Test
    public void testAddTab3() {
        /* Tab 2, 3 use Tab0 as parent */

        String tabId0 = session.addTab(null, "url0", false, false);
        String tabId1 = session.addTab(null, "url1", false, false);
        String tabId2 = session.addTab(tabId0, "url2", false, false);
        String tabId3 = session.addTab(tabId0, "url3", false, false);

        // tabId0 -> tabId3 -> tabId2 -> tabId1
        Assert.assertEquals(session.getTabs().get(0).getId(), tabId0);
        Assert.assertEquals(session.getTabs().get(1).getId(), tabId3);
        Assert.assertEquals(session.getTabs().get(2).getId(), tabId2);
        Assert.assertEquals(session.getTabs().get(3).getId(), tabId1);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));
        Assert.assertEquals(session.getTabs().get(1).getParentId(), tabId0);
        Assert.assertEquals(session.getTabs().get(2).getParentId(), tabId3);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(3).getParentId()));
    }

    @Test
    public void testAddTab4() {
        session.restoreTabs(models, urls[0]);
        Assert.assertEquals(session.getFocusTab().getId(), urls[0]);

        final String tabId0 = session.addTab(null, "url0", true, false);
        Assert.assertEquals(session.getFocusTab().getId(), tabId0);

        final String tabId1 = session.addTab(null, "url1", false, true);
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);

        final String tabId2 = session.addTab(null, "url2", false, false);
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);
    }

    @Test
    public void testRestore() {
        session.restoreTabs(models, urls[0]);
        Assert.assertEquals(session.getTabs().size(), urls.length);
        session.addTab(null, "url0", false, false);
    }

    @Implements(Tab.class)
    public static class ShadowTab {

        @RealObject
        Tab realTab;

        @Implementation
        public TabView createView(@NonNull final Activity activity) {
            TabView tv = ReflectionHelpers.<TabView>getField(this.realTab, "tabView");
            if (tv == null) {
                tv = new DefaultTabView();
                tv.setViewClient(ReflectionHelpers.<TabViewClient>getField(this.realTab, "tabViewClient"));
                tv.setChromeClient(ReflectionHelpers.<TabChromeClient>getField(this.realTab, "tabChromeClient"));
                tv.setDownloadCallback(ReflectionHelpers.<DownloadCallback>getField(this.realTab, "downloadCallback"));

                ReflectionHelpers.setField(this.realTab, "tabView", tv);
            }

            return tv;
        }
    }

    private static class DefaultTabView implements TabView {
        @Override
        public void setBlockingEnabled(boolean enabled) {

        }

        @Override
        public boolean isBlockingEnabled() {
            return false;
        }

        @Override
        public void performExitFullScreen() {

        }

        @Override
        public void setViewClient(@Nullable TabViewClient viewClient) {

        }

        @Override
        public void setChromeClient(@Nullable TabChromeClient chromeClient) {

        }

        @Override
        public void setDownloadCallback(DownloadCallback callback) {

        }

        @Override
        public void onPause() {

        }

        @Override
        public void onResume() {

        }

        @Override
        public void destroy() {

        }

        @Override
        public void reload() {

        }

        @Override
        public void stopLoading() {

        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public @SiteIdentity.SecurityState int getSecurityState() {
            return SiteIdentity.UNKNOWN;
        }

        @Override
        public void loadUrl(String url) {

        }

        @Override
        public void cleanup() {

        }

        @Override
        public void goForward() {

        }

        @Override
        public void goBack() {

        }

        @Override
        public boolean canGoForward() {
            return false;
        }

        @Override
        public boolean canGoBack() {
            return false;
        }

        @Override
        public void restoreViewState(Bundle inState) {

        }

        @Override
        public void saveViewState(Bundle outState) {

        }

        @Override
        public void insertBrowsingHistory() {

        }

        @Override
        public View getView() {
            return null;
        }

        @Override
        public void buildDrawingCache(boolean autoScale) {

        }

        @Override
        public Bitmap getDrawingCache(boolean autoScale) {
            return null;
        }
    }

    private static class DefaultChromeListener implements TabsChromeListener {

        @Override
        public void onProgressChanged(@NonNull Tab tab, int progress) {

        }

        @Override
        public void onReceivedTitle(@NonNull Tab tab, String title) {

        }

        @Override
        public void onReceivedIcon(@NonNull Tab tab, Bitmap icon) {

        }

        @Override
        public void onTabHoist(@NonNull Tab tab, @Factor int factor) {

        }

        @Override
        public void onTabCountChanged(int count) {

        }

        @Override
        public void onLongPress(@NonNull Tab tab, TabView.HitTarget hitTarget) {

        }

        @Override
        public void onEnterFullScreen(@NonNull Tab tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {
        }

        @Override
        public void onExitFullScreen(@NonNull Tab tab) {

        }

        @Override
        public boolean onShowFileChooser(@NonNull Tab tab, WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab, String origin, GeolocationPermissions.Callback callback) {

        }
    }
}
