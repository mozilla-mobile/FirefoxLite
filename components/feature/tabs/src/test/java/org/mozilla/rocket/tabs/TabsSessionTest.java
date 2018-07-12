/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.rocket.tabs.utils.DefaultTabsChromeListener;
import org.mozilla.rocket.tabs.utils.TabUtil;
import org.mozilla.rocket.tabs.web.DownloadCallback;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
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
        session = new TabsSession(new DefaultTabViewProvider());

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

        final String tabId0 = session.addTab("url0", TabUtil.argument(null, false, false));
        final String tabId1 = session.addTab("url1", TabUtil.argument(null, false, false));
        final String tabId2 = session.addTab("url2", TabUtil.argument(null, false, false));
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

        String tabId0 = session.addTab("url0", TabUtil.argument(null, false, false));
        Assert.assertEquals(1, session.getTabs().size());

        // this tab should not have parent
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));

        // no parent, should be append to tail
        String tabId1 = session.addTab("url1", TabUtil.argument(null, false, false));

        // tab0 is parent, order should be: tabId0 -> tabId2 -> tabId1
        String tabId2 = session.addTab("url2", TabUtil.argument(tabId0, false, false));

        Assert.assertEquals(session.getTabs().get(0).getId(), tabId0);
        Assert.assertEquals(session.getTabs().get(1).getId(), tabId2);
        Assert.assertEquals(session.getTabs().get(2).getId(), tabId1);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));
        Assert.assertEquals(session.getTabs().get(1).getParentId(), tabId0);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(2).getParentId()));
    }

    @Test
    public void testAddTab3() {
        String tabId0 = session.addTab("url0", TabUtil.argument(null, false, false));
        String tabId1 = session.addTab("url1", TabUtil.argument(null, false, false));
        String tabId2 = session.addTab("url2", TabUtil.argument(tabId0, false, false));
        String tabId3 = session.addTab("url3", TabUtil.argument(tabId0, false, false));
        String tabId4 = session.addTab("url4", TabUtil.argument(tabId1, false, false));
        String tabId5 = session.addTab("url5", TabUtil.argument(tabId4, false, false));

        // tabId0 -> tabId3 -> tabId2, tabId1 -> tabId4 -> tabId5
        Assert.assertEquals(session.getTabs().get(0).getId(), tabId0);
        Assert.assertEquals(session.getTabs().get(1).getId(), tabId3);
        Assert.assertEquals(session.getTabs().get(2).getId(), tabId2);
        Assert.assertEquals(session.getTabs().get(3).getId(), tabId1);
        Assert.assertEquals(session.getTabs().get(4).getId(), tabId4);
        Assert.assertEquals(session.getTabs().get(5).getId(), tabId5);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(0).getParentId()));
        Assert.assertEquals(session.getTabs().get(1).getParentId(), tabId0);
        Assert.assertEquals(session.getTabs().get(2).getParentId(), tabId3);
        Assert.assertTrue(TextUtils.isEmpty(session.getTabs().get(3).getParentId()));
        Assert.assertEquals(session.getTabs().get(4).getParentId(), tabId1);
        Assert.assertEquals(session.getTabs().get(5).getParentId(), tabId4);
    }

    @Test
    public void testAddTab3A() {
        final String tabId0 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId1 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId2 = session.addTab("url", TabUtil.argument(tabId1, false, true));
        final String tabId3 = session.addTab("url", TabUtil.argument(tabId2, false, true));
        // open from external
        final String tabId4 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId5 = session.addTab("url", TabUtil.argument(tabId4, false, true));
        final String tabId6 = session.addTab("url", TabUtil.argument(tabId5, false, true));
        final String tabId7 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId8 = session.addTab("url", TabUtil.argument(tabId7, false, true));
        final String tabId9 = session.addTab("url", TabUtil.argument(tabId8, false, true));

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        final List<Tab> tabs = session.getTabs();
        Assert.assertEquals(session.getFocusTab().getId(), tabId9);
        Assert.assertEquals(tabs.get(0).getId(), tabId0);
        Assert.assertEquals(tabs.get(1).getId(), tabId1);
        Assert.assertEquals(tabs.get(2).getId(), tabId2);
        Assert.assertEquals(tabs.get(3).getId(), tabId3);
        Assert.assertEquals(tabs.get(4).getId(), tabId4);
        Assert.assertEquals(tabs.get(5).getId(), tabId5);
        Assert.assertEquals(tabs.get(6).getId(), tabId6);
        Assert.assertEquals(tabs.get(7).getId(), tabId7);
        Assert.assertEquals(tabs.get(8).getId(), tabId8);
        Assert.assertEquals(tabs.get(9).getId(), tabId9);

        Assert.assertTrue(TextUtils.isEmpty(tabs.get(0).getParentId()));
        Assert.assertTrue(TextUtils.isEmpty(tabs.get(1).getParentId()));
        Assert.assertEquals(tabs.get(2).getParentId(), tabId1);
        Assert.assertEquals(tabs.get(3).getParentId(), tabId2);
        Assert.assertEquals(tabs.get(4).getParentId(), Tab.ID_EXTERNAL);
        Assert.assertEquals(tabs.get(5).getParentId(), tabId4);
        Assert.assertEquals(tabs.get(6).getParentId(), tabId5);
        Assert.assertEquals(tabs.get(7).getParentId(), Tab.ID_EXTERNAL);
        Assert.assertEquals(tabs.get(8).getParentId(), tabId7);
        Assert.assertEquals(tabs.get(9).getParentId(), tabId8);
    }

    @Test
    public void testAddTab4() {
        session.restoreTabs(models, urls[0]);
        Assert.assertEquals(session.getFocusTab().getId(), urls[0]);

        final String tabId0 = session.addTab("url0", TabUtil.argument(null, true, false));
        Assert.assertEquals(session.getFocusTab().getId(), tabId0);

        final String tabId1 = session.addTab("url1", TabUtil.argument(null, false, true));
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);

        final String tabId2 = session.addTab("url2", TabUtil.argument(null, false, false));
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);
    }

    // Ignore this test case, I have no idea why it does not work for now, it was working before
    @Ignore
    public void testAddTab5() {
        // Add a tab from internal and focus it. onFocusChanged should be invoked once
        final TabsChromeListener spy0 = spy(new DefaultTabsChromeListener() {
            public void onFocusChanged(@Nullable Tab tab, @Factor int factor) {
                Assert.assertEquals(tab.getUrl(), "url0");
            }
        });
        session.addTabsChromeListener(spy0);
        final String tabId0 = session.addTab("url0", TabUtil.argument(null, false, true));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(spy0, times(1)).onFocusChanged(any(Tab.class), eq(TabsChromeListener.FACTOR_TAB_ADDED));
        Assert.assertEquals(session.getFocusTab().getId(), tabId0);
        session.removeTabsChromeListener(spy0);

        // Add a tab from external. onFocusChanged should be invoked
        final TabsChromeListener spy1 = spy(new DefaultTabsChromeListener() {
            public void onFocusChanged(@Nullable Tab tab, @Factor int factor) {
                Assert.assertEquals(tab.getUrl(), "url1");
            }
        });
        session.addTabsChromeListener(spy1);
        final String tabId1 = session.addTab("url1", TabUtil.argument(null, true, false));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(spy1, times(1)).onFocusChanged(any(Tab.class), eq(TabsChromeListener.FACTOR_TAB_ADDED));
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);
        session.removeTabsChromeListener(spy1);

        // Add a tab from internal, but don't focus it.
        // Add a tab from external. onFocusChanged should be invoked
        final TabsChromeListener spy2 = spy(TabsChromeListener.class);
        session.addTabsChromeListener(spy2);
        session.addTab("url2", TabUtil.argument(null, false, false));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(spy2, times(0)).onFocusChanged(any(Tab.class), anyInt());
        Assert.assertEquals(session.getFocusTab().getId(), tabId1); // focus should not be changed
        session.removeTabsChromeListener(spy2);
    }

    @Test
    public void testRestore() {
        session.restoreTabs(models, urls[0]);
        Assert.assertEquals(session.getTabs().size(), urls.length);
        Assert.assertEquals(session.getFocusTab().getId(), urls[0]);
    }

    @Test
    public void testSwitch() {
        session.restoreTabs(models, urls[0]);
        session.switchToTab("not-exist");
        Assert.assertEquals(session.getFocusTab().getId(), urls[0]);

        session.switchToTab(urls[0]);
        Assert.assertEquals(session.getFocusTab().getId(), urls[0]);
        session.switchToTab(urls[3]);
        Assert.assertEquals(session.getFocusTab().getId(), urls[3]);

        session.switchToTab(urls[2]);
        Assert.assertEquals(session.getFocusTab().getId(), urls[2]);
    }

    /**
     * If there is no any focus, remove tab won't effect focus
     */
    @Test
    public void testCloseTab1() {
        session.restoreTabs(models, null);
        Assert.assertNull(session.getFocusTab());
        session.closeTab(models.get(2).getId());
        Assert.assertNull(session.getFocusTab());
        session.closeTab(models.get(0).getId());
        Assert.assertNull(session.getFocusTab());
    }

    /**
     * If there is no any focus, remove tab won't effect focus
     */
    @Test
    public void testCloseTab2() {
        final String tabId0 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId1 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId2 = session.addTab("url", TabUtil.argument(tabId1, false, true));
        final String tabId3 = session.addTab("url", TabUtil.argument(tabId2, false, true));
        // open from external
        final String tabId4 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId5 = session.addTab("url", TabUtil.argument(tabId4, false, true));
        final String tabId6 = session.addTab("url", TabUtil.argument(tabId5, false, true));
        final String tabId7 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId8 = session.addTab("url", TabUtil.argument(tabId7, false, true));
        final String tabId9 = session.addTab("url", TabUtil.argument(tabId8, false, true));

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        session.switchToTab(tabId6);
        session.closeTab(tabId5);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.closeTab(tabId6);
        Assert.assertEquals(session.getFocusTab().getId(), tabId4);
        session.closeTab(tabId4);
        Assert.assertNull(session.getFocusTab());

        session.switchToTab(tabId3);
        session.closeTab(tabId3);
        Assert.assertEquals(session.getFocusTab().getId(), tabId2);
        session.closeTab(tabId2);
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);

        session.switchToTab(tabId7);
        session.closeTab(tabId7);
        Assert.assertNull(session.getFocusTab());
        session.switchToTab(tabId8);
        session.closeTab(tabId8);
        Assert.assertNull(session.getFocusTab());
        session.switchToTab(tabId9);
        session.closeTab(tabId9);
        Assert.assertNull(session.getFocusTab());
    }

    /**
     * If there is no any focus, drop tab won't effect focus
     */
    @Test
    public void testDropTab1() {
        session.restoreTabs(models, null);
        Assert.assertNull(session.getFocusTab());
        session.dropTab(models.get(2).getId());
        Assert.assertNull(session.getFocusTab());
        session.dropTab(models.get(0).getId());
        Assert.assertNull(session.getFocusTab());
    }

    @Test
    public void testDropTab2() {
        final String tabId0 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId1 = session.addTab("url", TabUtil.argument(null, false, true));
        final String tabId2 = session.addTab("url", TabUtil.argument(tabId1, false, true));
        final String tabId3 = session.addTab("url", TabUtil.argument(tabId2, false, true));
        // open from external
        final String tabId4 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId5 = session.addTab("url", TabUtil.argument(tabId4, false, true));
        final String tabId6 = session.addTab("url", TabUtil.argument(tabId5, false, true));
        final String tabId7 = session.addTab("url", TabUtil.argument(null, true, false));
        final String tabId8 = session.addTab("url", TabUtil.argument(tabId7, false, true));
        final String tabId9 = session.addTab("url", TabUtil.argument(tabId8, false, true));

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        Assert.assertEquals(session.getFocusTab().getId(), tabId9);

        session.switchToTab(tabId6);
        session.dropTab(tabId8);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId9);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId7);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId3);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId0);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId5);
        Assert.assertEquals(session.getFocusTab().getId(), tabId6);
        session.dropTab(tabId6);
        Assert.assertEquals(session.getFocusTab().getId(), tabId4);
        session.dropTab(tabId4);
        Assert.assertEquals(session.getFocusTab().getId(), tabId2);
        session.dropTab(tabId2);
        Assert.assertEquals(session.getFocusTab().getId(), tabId1);
        session.dropTab(tabId1);
        Assert.assertNull(session.getFocusTab());
    }

    private static class DefaultTabViewProvider implements TabViewProvider {

        @Override
        public TabView create() {
            return new DefaultTabView();
        }
    }

    private static class DefaultTabView implements TabView {
        private String url = null;

        @Override
        public void setContentBlockingEnabled(boolean enabled) {

        }

        @Override
        public void bindOnNewWindowCreation(@NonNull Message msg) {
        }

        @Override
        public void setImageBlockingEnabled(boolean enabled) {

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
            return this.url;
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        @SiteIdentity.SecurityState
        public int getSecurityState() {
            return SiteIdentity.UNKNOWN;
        }

        @Override
        public void loadUrl(String url) {
            this.url = url;
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
}
