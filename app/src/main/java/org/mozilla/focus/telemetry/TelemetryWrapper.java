/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.telemetry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.PermissionRequest;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.focus.utils.DebugUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.event.TelemetryEvent;
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement;
import org.mozilla.telemetry.measurement.SearchesMeasurement;
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient;
import org.mozilla.telemetry.net.TelemetryClient;
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder;
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder;
import org.mozilla.telemetry.schedule.TelemetryScheduler;
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler;
import org.mozilla.telemetry.serialize.JSONPingSerializer;
import org.mozilla.telemetry.serialize.TelemetryPingSerializer;
import org.mozilla.telemetry.storage.FileTelemetryStorage;
import org.mozilla.telemetry.storage.TelemetryStorage;

import java.util.HashMap;

import static org.mozilla.focus.telemetry.TelemetryWrapper.Value.SETTINGS;

public final class TelemetryWrapper {
    static final String TELEMETRY_APP_NAME_ZERDA = "Zerda";

    private static final int TOOL_BAR_CAPTURE_TELEMETRY_VERSION = 2;
    private static final int RATE_APP_NOTIFICATION_TELEMETRY_VERSION = 2;
    private static final int DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION = 2;

    private TelemetryWrapper() {
    }

    static class Category {
        static final String ACTION = "action";
    }

    static class Method {
        static final String TYPE_QUERY = "type_query";
        static final String TYPE_SELECT_QUERY = "select_query";
        static final String CLICK = "click";
        static final String CANCEL = "cancel";
        static final String LONG_PRESS = "long_press";
        static final String CHANGE = "change";
        static final String CLEAR = "clear";
        static final String REMOVE = "remove";
        static final String DELETE = "delete";
        static final String EDIT = "edit";
        static final String PERMISSION = "permission";
        static final String FULLSCREEN = "fullscreen";
        static final String ADD = "add";
        static final String SWIPE = "swipe";

        static final String FOREGROUND = "foreground";
        static final String BACKGROUND = "background";
        static final String SHARE = "share";
        static final String PIN_SHORTCUT = "pin_shortcut";
        static final String SAVE = "save";
        static final String COPY = "copy";
        static final String OPEN = "open";
        static final String INTENT_URL = "intent_url";
        static final String TEXT_SELECTION_INTENT = "text_selection_intent";
        static final String SHOW = "show";
        static final String LAUNCH = "launch";
    }

    static class Object {
        static final String PANEL = "panel";
        static final String TOOLBAR = "toolbar";
        static final String HOME = "home";
        static final String CAPTURE = "capture";
        static final String SEARCH_SUGGESTION = "search_suggestion";
        static final String SEARCH_BAR = "search_bar";
        static final String TAB = "tab";
        static final String TABTRAY = "tab_tray";
        static final String CLOSE_ALL = "close_all";

        static final String SETTING = "setting";
        static final String APP = "app";
        static final String MENU = "menu";

        static final String BROWSER = "browser";
        static final String BROWSER_CONTEXTMENU = "browser_contextmenu";
        static final String FIRSTRUN = "firstrun";

        static final String FEEDBACK = "feedback";
        static final String DEFAULT_BROWSER = "default_browser";
        static final String PROMOTE_SHARE = "promote_share";
    }

    public static class Value {
        static final String HOME = "home";
        static final String TOPSITE = "top_site";
        static final String DOWNLOAD = "download";
        static final String HISTORY = "history";
        static final String TURBO = "turbo";
        static final String BLOCK_IMAGE = "block_image";
        static final String CLEAR_CACHE = "clear_cache";
        static final String SETTINGS = "settings";

        static final String TABTRAY = "tab_tray";
        static final String TOOLBAR = "toolbar";
        static final String FORWARD = "forward";
        static final String RELOAD = "reload";
        static final String CAPTURE = "capture";
        static final String BOOKMARK = "bookmark";

        static final String SEARCH_BUTTON = "search_btn";
        static final String SEARCH_BOX = "search_box";
        static final String MINI_URLBAR = "mini_urlbar";

        static final String FILE = "file";
        static final String IMAGE = "image";
        static final String LINK = "link";
        static final String FINISH = "finish";
        static final String INFO = "info";

        static final String ENTER = "enter";
        static final String EXIT = "exit";
        static final String GEOLOCATION = "geolocation";
        static final String AUDIO = "audio";
        static final String VIDEO = "video";
        static final String MIDI = "midi";
        static final String EME = "eme";

        static final String LEARN_MORE = "learn_more";

        public static final String DISMISS = "dismiss";
        public static final String POSITIVE = "positive";
        public static final String NEGATIVE = "negative";
        public static final String SHARE = "share";

        static final String LAUNCHER = "launcher";
        static final String EXTERNAL_APP = "external_app";
        static final String SHORTCUT = "shortcut";
    }

    static class Extra {
        static final String TO = "to";
        static final String ON = "on";
        static final String DEFAULT = "default";
        static final String SUCCESS = "success";
        static final String SNACKBAR = "snackbar";
        static final String SOURCE = "source";
        static final String VERSION = "version";
        static final String TYPE = "type";
    }

    public static class Extra_Value {
        public static final String SETTING = "settings";
        public static final String CONTEXTUAL_HINTS = "contextual_hints";
        public static final String NOTIFICATION = "notification";
        static final String TEXT_SELECTION = "text_selection";
    }

    public static boolean isTelemetryEnabled(Context context) {
        return Inject.isTelemetryEnabled(context);
    }

    public static void setTelemetryEnabled(Context context, boolean enabled) {
        final Resources resources = context.getResources();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferences.edit()
                .putBoolean(resources.getString(R.string.pref_key_telemetry), enabled)
                .apply();

        TelemetryHolder.get()
                .getConfiguration()
                .setUploadEnabled(enabled)
                .setCollectionEnabled(enabled);
    }

    public static void init(Context context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        final StrictMode.ThreadPolicy threadPolicy = StrictMode.allowThreadDiskWrites();
        try {
            final Resources resources = context.getResources();

            final boolean telemetryEnabled = isTelemetryEnabled(context);

            updateDefaultBrowserStatus(context);
            updatePrefValue(context, resources.getString(R.string.pref_key_webview_version), DebugUtils.loadWebViewVersion(context));

            final TelemetryConfiguration configuration = new TelemetryConfiguration(context)
                    .setServerEndpoint("https://incoming.telemetry.mozilla.org")
                    .setAppName(TELEMETRY_APP_NAME_ZERDA)
                    .setUpdateChannel(BuildConfig.BUILD_TYPE)
                    .setPreferencesImportantForTelemetry(
                            resources.getString(R.string.pref_key_search_engine),
                            resources.getString(R.string.pref_key_turbo_mode),
                            resources.getString(R.string.pref_key_performance_block_images),
                            resources.getString(R.string.pref_key_default_browser),
                            resources.getString(R.string.pref_key_storage_save_downloads_to),
                            resources.getString(R.string.pref_key_webview_version),
                            resources.getString(R.string.pref_key_locale))
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(telemetryEnabled);

            final TelemetryPingSerializer serializer = new JSONPingSerializer();
            final TelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);
            final TelemetryClient client = new HttpURLConnectionTelemetryClient();
            final TelemetryScheduler scheduler = new JobSchedulerTelemetryScheduler();

            TelemetryHolder.set(new Telemetry(configuration, storage, client, scheduler)
                    .addPingBuilder(new TelemetryCorePingBuilder(configuration))
                    .addPingBuilder(new TelemetryEventPingBuilder(configuration))
                    .setDefaultSearchProvider(createDefaultSearchProvider(context)));
        } finally {
            StrictMode.setThreadPolicy(threadPolicy);
        }
    }

    private static void updateDefaultBrowserStatus(Context context) {
        Settings.updatePrefDefaultBrowserIfNeeded(context, Browsers.isDefaultBrowser(context));
    }

    private static void updatePrefValue(Context context, String key, String value) {
        Settings.updatePrefString(context, key, value);
    }

    private static DefaultSearchMeasurement.DefaultSearchEngineProvider createDefaultSearchProvider(final Context context) {
        return new DefaultSearchMeasurement.DefaultSearchEngineProvider() {
            @Override
            public String getDefaultSearchEngineIdentifier() {
                return SearchEngineManager.getInstance()
                        .getDefaultSearchEngine(context)
                        .getIdentifier();
            }
        };
    }

    public static void toggleFirstRunPageEvent(boolean enableTurboMode) {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.FIRSTRUN, Value.TURBO)
                .extra(Extra.TO, Boolean.toString(enableTurboMode))
                .queue();
    }

    public static void finishFirstRunEvent(long duration) {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN, Value.FINISH)
                .extra(Extra.ON, Long.toString(duration))
                .queue();
    }

    public static void browseIntentEvent() {
        new EventBuilder(Category.ACTION, Method.INTENT_URL, Object.APP).queue();
    }

    public static void textSelectionIntentEvent() {
        new EventBuilder(Category.ACTION, Method.TEXT_SELECTION_INTENT, Object.APP).queue();
    }

    public static void launchByAppLauncherEvent() {
        new EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.LAUNCHER).queue();
    }

    public static void launchByHomeScreenShortcutEvent() {
        new EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.SHORTCUT).queue();
    }

    public static void launchByTextSelectionSearchEvent() {
        new EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                .extra(Extra.TYPE, Extra_Value.TEXT_SELECTION)
                .queue();
    }

    public static void launchByExternalAppEvent() {
        new EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP).queue();
    }

    public static void settingsEvent(String key, String value) {
        // We only log whitelist-ed setting
        final String validPrefKey = FirebaseEvent.getValidPrefKey(key);
        if (validPrefKey != null) {
            new EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, validPrefKey)
                    .extra(Extra.TO, value)
                    .queue();
        }

    }

    public static void settingsClickEvent(String key) {
        final String validPrefKey = FirebaseEvent.getValidPrefKey(key);
        if (validPrefKey != null) {
            new EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, key).queue();
        }
    }

    public static void settingsLearnMoreClickEvent(String source) {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, Value.LEARN_MORE)
                .extra(Extra.SOURCE, source)
                .queue();
    }

    public static void settingsLocaleChangeEvent(String key, String value, boolean isDefault) {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .extra(Extra.DEFAULT, Boolean.toString(isDefault))
                .queue();
    }

    public static void startSession() {
        TelemetryHolder.get().recordSessionStart();

        new EventBuilder(Category.ACTION, Method.FOREGROUND, Object.APP).queue();
    }

    public static void stopSession() {
        TelemetryHolder.get().recordSessionEnd();

        new EventBuilder(Category.ACTION, Method.BACKGROUND, Object.APP).queue();
    }

    public static void stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryEventPingBuilder.TYPE)
                .scheduleUpload();
    }

    public static void openWebContextMenuEvent() {
        new EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.BROWSER).queue();
    }

    public static void cancelWebContextMenuEvent() {
        new EventBuilder(Category.ACTION, Method.CANCEL, Object.BROWSER_CONTEXTMENU).queue();
    }

    public static void shareLinkEvent() {
        new EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.LINK).queue();
    }

    public static void shareImageEvent() {
        new EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void saveImageEvent() {
        new EventBuilder(Category.ACTION, Method.SAVE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void copyLinkEvent() {
        new EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.LINK).queue();
    }

    public static void copyImageEvent() {
        new EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void addNewTabFromContextMenu() {
        new EventBuilder(Category.ACTION, Method.ADD, Object.BROWSER_CONTEXTMENU, Value.LINK).queue();
    }

    public static void browseGeoLocationPermissionEvent() {
        new EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.GEOLOCATION).queue();
    }

    public static void browseFilePermissionEvent() {
        new EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.FILE).queue();
    }

    public static void browsePermissionEvent(String[] requests) {
        for (String request : requests) {
            final String value;
            switch (request) {
                case PermissionRequest.RESOURCE_AUDIO_CAPTURE:
                    value = Value.AUDIO;
                    break;
                case PermissionRequest.RESOURCE_VIDEO_CAPTURE:
                    value = Value.VIDEO;
                    break;
                case PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID:
                    value = Value.EME;
                    break;
                case PermissionRequest.RESOURCE_MIDI_SYSEX:
                    value = Value.MIDI;
                    break;
                default:
                    value = request;
                    break;
            }
            new EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, value).queue();
        }
    }

    public static void browseEnterFullScreenEvent() {
        new EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.ENTER).queue();
    }

    public static void browseExitFullScreenEvent() {
        new EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.EXIT).queue();
    }

    public static void showMenuHome() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HOME).queue();
    }

    public static void showTabTrayHome() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.HOME).queue();
    }

    public static void showTabTrayToolbar() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.TOOLBAR).queue();
    }

    public static void showMenuToolbar() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.TOOLBAR).queue();
    }

    public static void clickMenuDownload() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.DOWNLOAD).queue();
    }

    public static void clickMenuHistory() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.HISTORY).queue();
    }

    public static void clickMenuCapture() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CAPTURE).queue();
    }

    public static void showPanelBookmark() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.BOOKMARK).queue();
    }

    public static void showPanelDownload() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.DOWNLOAD).queue();
    }

    public static void showPanelHistory() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.HISTORY).queue();
    }

    public static void showPanelCapture() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.CAPTURE).queue();
    }

    public static void menuTurboChangeTo(boolean enable) {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.TURBO)
                .extra(Extra.TO, Boolean.toString(enable))
                .queue();
    }

    public static void menuBlockImageChangeTo(boolean enable) {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.BLOCK_IMAGE)
                .extra(Extra.TO, Boolean.toString(enable))
                .queue();
    }

    public static void clickMenuClearCache() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CLEAR_CACHE).queue();
    }

    public static void clickMenuSettings() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, SETTINGS).queue();
    }

    public static void clickMenuExit() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.EXIT).queue();
    }

    public static void clickMenuBookmark() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.BOOKMARK).queue();
    }

    public static void clickToolbarForward() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.FORWARD).queue();
    }

    public static void clickToolbarReload() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD).queue();
    }

    public static void clickToolbarShare() {
        new EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK).queue();
    }

    public static void clickToolbarBookmark() {
        new EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.BOOKMARK).queue();
    }

    public static void clickAddToHome() {
        new EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.TOOLBAR, Value.LINK).queue();
    }

    public static void clickToolbarCapture() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.CAPTURE)
                .extra(Extra.VERSION, Integer.toString(TOOL_BAR_CAPTURE_TELEMETRY_VERSION))
                .queue();
    }

    public static void clickTopSiteOn(int index) {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, Integer.toString(index))
                .queue();

        new EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOPSITE)
                .queue();
    }

    public static void removeTopSite(boolean isDefault) {
        new EventBuilder(Category.ACTION, Method.REMOVE, Object.HOME, Value.LINK)
                .extra(Extra.DEFAULT, Boolean.toString(isDefault))
                //  TODO: add index
                .queue();
    }

    public static void addNewTabFromHome() {
        new EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.HOME).queue();
    }

    public static void urlBarEvent(boolean isUrl, boolean isSuggestion) {
        if (isUrl) {
            TelemetryWrapper.browseEvent();
        } else if (isSuggestion) {
            TelemetryWrapper.searchSelectEvent();
        } else {
            TelemetryWrapper.searchEnterEvent();
        }
    }

    private static void browseEvent() {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK).queue();
    }

    public static void searchSelectEvent() {
        Telemetry telemetry = TelemetryHolder.get();

        new EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR).queue();

        SearchEngine searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.getConfiguration().getContext());

        telemetry.recordSearch(SearchesMeasurement.LOCATION_SUGGESTION, searchEngine.getIdentifier());
    }

    private static void searchEnterEvent() {
        Telemetry telemetry = TelemetryHolder.get();

        new EventBuilder(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR).queue();

        SearchEngine searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.getConfiguration().getContext());

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.getIdentifier());
    }

    public static void searchSuggestionLongClick() {
        new EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.SEARCH_SUGGESTION).queue();
    }

    public static void searchClear() {
        new EventBuilder(Category.ACTION, Method.CLEAR, Object.SEARCH_BAR).queue();
    }

    public static void searchDismiss() {
        new EventBuilder(Category.ACTION, Method.CANCEL, Object.SEARCH_BAR).queue();
    }

    public static void showSearchBarHome() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BOX).queue();
    }

    public static void clickUrlbar() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.MINI_URLBAR).queue();
    }

    public static void clickToolbarSearch() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON).queue();
    }

    public static void clickAddTabToolbar() {
        new EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOOLBAR).queue();
    }

    public static void clickAddTabTray() {
        new EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TABTRAY).queue();
    }

    public static void clickTabFromTabTray() {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.TAB, Value.TABTRAY).queue();
    }

    public static void closeTabFromTabTray() {
        new EventBuilder(Category.ACTION, Method.REMOVE, Object.TAB, Value.TABTRAY).queue();
    }

    public static void swipeTabFromTabTray() {
        new EventBuilder(Category.ACTION, Method.SWIPE, Object.TAB, Value.TABTRAY).queue();
    }

    public static void closeAllTabFromTabTray() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.CLOSE_ALL, Value.TABTRAY).queue();
    }

    public static void downloadRemoveFile() {
        new EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.FILE).queue();
    }

    public static void downloadDeleteFile() {
        new EventBuilder(Category.ACTION, Method.DELETE, Object.PANEL, Value.FILE).queue();
    }

    public static void downloadOpenFile(boolean fromSnackBar) {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.FILE)
                .extra(Extra.SNACKBAR, Boolean.toString(fromSnackBar))
                .queue();
    }

    public static void showFileContextMenu() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.DOWNLOAD).queue();
    }

    public static void historyOpenLink() {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LINK).queue();
    }

    public static void historyRemoveLink() {
        new EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.LINK).queue();
    }

    public static void bookmarkRemoveItem() {
        new EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.BOOKMARK).queue();
    }

    public static void bookmarkEditItem() {
        new EventBuilder(Category.ACTION, Method.EDIT, Object.PANEL, Value.BOOKMARK).queue();
    }

    public static void bookmarkOpenItem() {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.BOOKMARK).queue();
    }

    public static void showHistoryContextMenu() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HISTORY).queue();
    }

    public static void showBookmarkContextMenu() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.BOOKMARK).queue();
    }

    public static void clearHistory() {
        new EventBuilder(Category.ACTION, Method.CLEAR, Object.PANEL, Value.HISTORY).queue();
    }

    public static void openCapture() {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.CAPTURE).queue();
    }

    public static void openCaptureLink() {
        new EventBuilder(Category.ACTION, Method.OPEN, Object.CAPTURE, Value.LINK).queue();
    }

    public static void editCaptureImage(boolean editAppResolved) {
        new EventBuilder(Category.ACTION, Method.EDIT, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SUCCESS, Boolean.toString(editAppResolved))
                .queue();
    }

    public static void shareCaptureImage(boolean fromSnackBar) {
        new EventBuilder(Category.ACTION, Method.SHARE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SNACKBAR, Boolean.toString(fromSnackBar))
                .queue();
    }

    public static void showCaptureInfo() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.CAPTURE, Value.INFO).queue();
    }

    public static void deleteCaptureImage() {
        new EventBuilder(Category.ACTION, Method.DELETE, Object.CAPTURE, Value.IMAGE).queue();
    }

    public static void feedbackClickEvent(String value, String source) {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, source)
                .queue();
    }

    public static void showFeedbackDialog() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK).queue();
    }

    public static void showRateAppNotification() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue();
    }

    // TODO: test Context from contetReceiver
    public static void clickRateAppNotification(String value) {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue();
    }

    public static void clickRateAppNotification() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, String.valueOf(RATE_APP_NOTIFICATION_TELEMETRY_VERSION))
                .queue();
    }

    public static void showDefaultSettingNotification() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue();
    }

    public static void clickDefaultSettingNotification() {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, Integer.toString(DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION))
                .queue();
    }

    public static void onDefaultBrowserServiceFailed() {
        new EventBuilder(Category.ACTION, Method.CHANGE, Object.DEFAULT_BROWSER)
                .extra(Extra.SUCCESS, Boolean.toString(false))
                .queue();
    }

    public static void promoteShareClickEvent(String value, String source) {
        new EventBuilder(Category.ACTION, Method.CLICK, Object.PROMOTE_SHARE, value)
                .extra(Extra.SOURCE, source)
                .queue();
    }

    public static void showPromoteShareDialog() {
        new EventBuilder(Category.ACTION, Method.SHOW, Object.PROMOTE_SHARE).queue();
    }

    static class EventBuilder {
        TelemetryEvent telemetryEvent;
        FirebaseEvent firebaseEvent;

        EventBuilder(@NonNull String category, @NonNull String method, @Nullable String object) {
            this(category, method, object, null);
        }

        EventBuilder(@NonNull String category, @NonNull String method, @Nullable String object, String value) {
            lazyInit();
            telemetryEvent = TelemetryEvent.create(category, method, object, value);
            firebaseEvent = FirebaseEvent.create(category, method, object, value);
        }

        EventBuilder extra(String key, String value) {
            telemetryEvent.extra(key, value);
            firebaseEvent.param(key, value);
            return this;
        }

        void queue() {

            final Context context = TelemetryHolder.get().getConfiguration().getContext();
            if (context != null) {
                telemetryEvent.queue();
                firebaseEvent.event(context);
            }
        }

        static void lazyInit() {

            if (FirebaseEvent.isInitialized()) {
                return;
            }
            final Context context = TelemetryHolder.get().getConfiguration().getContext();
            if (context == null) {
                return;
            }
            final HashMap<String, String> prefKeyWhitelist = new HashMap<>();
            prefKeyWhitelist.put(context.getString(R.string.pref_key_search_engine), "search_engine");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_privacy_block_ads), "privacy_ads");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_privacy_block_analytics), "privacy_analytics");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_privacy_block_social), "privacy_social");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_privacy_block_other), "privacy_other");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_turbo_mode), "turbo_mode");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_performance_block_webfonts), "block_webfonts");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_performance_block_images), "block_images");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_default_browser), "default_browser");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_telemetry), "telemetry");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_give_feedback), "give_feedback");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_share_with_friends), "share_with_friends");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_about), "key_about");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_help), "help");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_rights), "rights");

            prefKeyWhitelist.put(context.getString(R.string.pref_key_webview_version), "webview_version");
            // data saving
            prefKeyWhitelist.put(context.getString(R.string.pref_key_data_saving_block_ads), "saving_block_ads");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_data_saving_block_webfonts), "data_webfont");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_data_saving_block_images), "data_images");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_data_saving_block_tab_restore), "tab_restore");

            // storage and cache
            prefKeyWhitelist.put(context.getString(R.string.pref_key_storage_clear_browsing_data), "clear_browsing_data)");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_removable_storage_available_on_create), "remove_storage");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_storage_save_downloads_to), "save_downloads_to");
            prefKeyWhitelist.put(context.getString(R.string.pref_key_showed_storage_message), "storage_message)");

            // rate / share app already have telemetry

            // clear browsing data
            prefKeyWhitelist.put(context.getString(R.string.pref_value_clear_browsing_history), "clear_browsing_his");
            prefKeyWhitelist.put(context.getString(R.string.pref_value_clear_form_history), "clear_form_his");
            prefKeyWhitelist.put(context.getString(R.string.pref_value_clear_cookies), "clear_cookies");
            prefKeyWhitelist.put(context.getString(R.string.pref_value_clear_cache), "clear_cache");

            // data saving path values
            prefKeyWhitelist.put(context.getString(R.string.pref_value_saving_path_sd_card), "path_sd_card");
            prefKeyWhitelist.put(context.getString(R.string.pref_value_saving_path_internal_storage), "path_internal_storage");

            //  default browser already have telemetry

            // NewFeatureNotice already have telemetry

            FirebaseEvent.setPrefKeyWhitelist(prefKeyWhitelist);

        }
    }
}
