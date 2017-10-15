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
import android.webkit.PermissionRequest;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.AppConstants;
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

public final class TelemetryWrapper {
    private static final String TELEMETRY_APP_NAME_ZERDA = "Zerda";

    private TelemetryWrapper() {}

    private static class Category {
        private static final String ACTION = "action";
    }

    private static class Method {
        private static final String TYPE_QUERY = "type_query";
        private static final String TYPE_SELECT_QUERY = "select_query";
        private static final String CLICK = "click";
        private static final String CANCEL = "cancel";
        private static final String LONG_PRESS = "long_press";
        private static final String CHANGE = "change";
        private static final String CLEAR = "clear";
        private static final String REMOVE = "remove";
        private static final String DELETE = "delete";
        private static final String EDIT = "edit";
        private static final String PERMISSION = "permission";
        private static final String FULLSCREEN = "fullscreen";

        private static final String FOREGROUND = "foreground";
        private static final String BACKGROUND = "background";
        private static final String SHARE = "share";
        private static final String SAVE = "save";
        private static final String COPY = "copy";
        private static final String OPEN = "open";
        private static final String INTENT_URL = "intent_url";
        private static final String TEXT_SELECTION_INTENT = "text_selection_intent";
        private static final String SHOW = "show";
    }

    private static class Object {
        private static final String PANEL = "panel";
        private static final String TOOLBAR = "toolbar";
        private static final String HOME = "home";
        private static final String CAPTURE = "capture";
        private static final String SEARCH_SUGGESTION = "search_suggestion";
        private static final String SEARCH_BAR = "search_bar";

        private static final String SETTING = "setting";
        private static final String APP = "app";
        private static final String MENU = "menu";

        private static final String BROWSER = "browser";
        private static final String BROWSER_CONTEXTMENU = "browser_contextmenu";
        private static final String FIRSTRUN = "firstrun";
    }

    private static class Value {
        private static final String HOME = "home";
        private static final String DOWNLOAD = "download";
        private static final String HISTORY = "history";
        private static final String TURBO = "turbo";
        private static final String BLOCK_IMAGE = "block_image";
        private static final String CLEAR_CACHE = "clear_cache";
        private static final String SETTINGS = "settings";

        private static final String TOOLBAR = "toolbar";
        private static final String FORWARD = "forward";
        private static final String RELOAD = "reload";
        private static final String CAPTURE = "capture";

        private static final String SEARCH_BUTTON = "search_btn";
        private static final String SEARCH_BOX = "search_box";
        private static final String MINI_URLBAR = "mini_urlbar";

        private static final String FILE = "file";
        private static final String IMAGE = "image";
        private static final String LINK = "link";
        private static final String FINISH = "finish";
        private static final String INFO = "info";

        private static final String ENTER = "enter";
        private static final String EXIT = "exit";
        private static final String GEOLOCATION = "geolocation";
        private static final String AUDIO = "audio";
        private static final String VIDEO = "video";
        private static final String MIDI = "midi";
        private static final String EME = "eme";
    }

    private static class Extra {
        private static final String TO = "to";
        private static final String ON = "on";
        private static final String DEFAULT = "default";
        private static final String SUCCESS = "success";
        private static final String SNACKBAR = "snackbar";
    }

    public static boolean isTelemetryEnabled(Context context) {
        // The first access to shared preferences will require a disk read.
        final StrictMode.ThreadPolicy threadPolicy = StrictMode.allowThreadDiskReads();
        try {
            final Resources resources = context.getResources();
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            return preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), isEnabledByDefault())
                    && !AppConstants.isDevBuild();
        } finally {
            StrictMode.setThreadPolicy(threadPolicy);
        }
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

    private static boolean isEnabledByDefault() {
        return AppConstants.isBetaBuild() || AppConstants.isReleaseBuild();
    }

    public static void init(Context context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        final StrictMode.ThreadPolicy threadPolicy = StrictMode.allowThreadDiskWrites();
        try {
            final Resources resources = context.getResources();

            final boolean telemetryEnabled = isTelemetryEnabled(context);

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
                            //  TODO:[Telemetry][P2] webview_version
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
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.FIRSTRUN, Value.TURBO)
                .extra(Extra.TO, Boolean.toString(enableTurboMode))
                .queue();
    }

    public static void finishFirstRunEvent(long duration) {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.FIRSTRUN, Value.FINISH)
                .extra(Extra.ON, Long.toString(duration))
                .queue();
    }

    public static void browseIntentEvent() {
        TelemetryEvent.create(Category.ACTION, Method.INTENT_URL, Object.APP).queue();
    }

    public static void textSelectionIntentEvent() {
        TelemetryEvent.create(Category.ACTION, Method.TEXT_SELECTION_INTENT, Object.APP).queue();
    }

    public static void settingsEvent(String key, String value) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .queue();
    }

    public static void settingsClickEvent(String key) {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.SETTING, key).queue();
    }

    public static void settingsLocaleChangeEvent(String key, String value, boolean isDefault) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .extra(Extra.DEFAULT, Boolean.toString(isDefault))
                .queue();
    }

    public static void startSession() {
        TelemetryHolder.get().recordSessionStart();

        TelemetryEvent.create(Category.ACTION, Method.FOREGROUND, Object.APP).queue();
    }

    public static void stopSession() {
        TelemetryHolder.get().recordSessionEnd();

        TelemetryEvent.create(Category.ACTION, Method.BACKGROUND, Object.APP).queue();
    }

    public static void stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryEventPingBuilder.TYPE)
                .scheduleUpload();
    }

    public static void openWebContextMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.LONG_PRESS, Object.BROWSER).queue();
    }

    public static void cancelWebContextMenuEvent() {
        TelemetryEvent.create(Category.ACTION, Method.CANCEL, Object.BROWSER_CONTEXTMENU).queue();
    }

    public static void shareLinkEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.LINK).queue();
    }

    public static void shareImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void saveImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.SAVE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void copyLinkEvent() {
        TelemetryEvent.create(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.LINK).queue();
    }

    public static void copyImageEvent() {
        TelemetryEvent.create(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue();
    }

    public static void browseGeoLocationPermissionEvent() {
        TelemetryEvent.create(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.GEOLOCATION).queue();
    }

    public static void browseFilePermissionEvent() {
        TelemetryEvent.create(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.FILE).queue();
    }

    public static void browsePermissionEvent(String[] requests) {
        for(String request : requests) {
            final String value;
            switch(request) {
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
            TelemetryEvent.create(Category.ACTION, Method.PERMISSION, Object.BROWSER, value).queue();
        }
    }

    public static void browseEnterFullScreenEvent() {
        TelemetryEvent.create(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.ENTER).queue();
    }

    public static void browseExitFullScreenEvent() {
        TelemetryEvent.create(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.EXIT).queue();
    }

    public static void showMenuHome() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.MENU, Value.HOME).queue();
    }

    public static void showMenuToolbar() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.MENU, Value.TOOLBAR).queue();
    }

    public static void clickMenuDownload() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.DOWNLOAD).queue();
    }

    public static void clickMenuHistory() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.HISTORY).queue();
    }

    public static void clickMenuCapture() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.CAPTURE).queue();
    }

    public static void showPanelDownload() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.PANEL, Value.DOWNLOAD).queue();
    }

    public static void showPanelHistory() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.PANEL, Value.HISTORY).queue();
    }

    public static void showPanelCapture() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.PANEL, Value.CAPTURE).queue();
    }

    public static void menuTurboChangeTo(boolean enable) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.MENU, Value.TURBO)
                .extra(Extra.TO, Boolean.toString(enable))
                .queue();
    }

    public static void menuBlockImageChangeTo(boolean enable) {
        TelemetryEvent.create(Category.ACTION, Method.CHANGE, Object.MENU, Value.BLOCK_IMAGE)
                .extra(Extra.TO, Boolean.toString(enable))
                .queue();
    }

    public static void clickMenuClearCache() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.CLEAR_CACHE).queue();
    }

    public static void clickMenuSettings() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.SETTINGS).queue();
    }

    public static void clickMenuExit() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.MENU, Value.EXIT).queue();
    }

    public static void clickToolbarForward() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.FORWARD).queue();
    }

    public static void clickToolbarReload() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD).queue();
    }

    public static void clickToolbarShare() {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK).queue();
    }

    public static void clickToolbarCapture() {
        TelemetryEvent.create(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.CAPTURE).queue();
    }

    public static void clickTopSiteOn(int index){
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, Integer.toString(index))
                .queue();
    }

    public static void removeTopSite(boolean isDefault) {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.HOME, Value.LINK)
                .extra(Extra.DEFAULT, Boolean.toString(isDefault))
                //  TODO: add index
                .queue();
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
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK).queue();
    }

    public static void searchSelectEvent() {
        Telemetry telemetry = TelemetryHolder.get();

        TelemetryEvent.create(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR).queue();

        SearchEngine searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.getConfiguration().getContext());

        telemetry.recordSearch(SearchesMeasurement.LOCATION_SUGGESTION, searchEngine.getIdentifier());
    }

    private static void searchEnterEvent() {
        Telemetry telemetry = TelemetryHolder.get();

        TelemetryEvent.create(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR).queue();

        SearchEngine searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.getConfiguration().getContext());

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.getIdentifier());
    }

    public static void searchSuggestionLongClick() {
        TelemetryEvent.create(Category.ACTION, Method.LONG_PRESS, Object.SEARCH_SUGGESTION).queue();
    }

    public static void searchClear() {
        TelemetryEvent.create(Category.ACTION, Method.CLEAR, Object.SEARCH_BAR).queue();
    }

    public static void searchDismiss() {
        TelemetryEvent.create(Category.ACTION, Method.CANCEL, Object.SEARCH_BAR).queue();
    }

    public static void showSearchBarHome() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BOX).queue();
    }

    public static void clickUrlbar() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.MINI_URLBAR).queue();
    }

    public static void clickToolbarSearch() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON).queue();
    }

    public static void clickToolbarHome() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.TOOLBAR, Value.HOME).queue();
    }

    public static void downloadRemoveFile() {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.PANEL, Value.FILE).queue();
    }

    public static void downloadDeleteFile() {
        TelemetryEvent.create(Category.ACTION, Method.DELETE, Object.PANEL, Value.FILE).queue();
    }

    public static void downloadOpenFile(boolean fromSnackBar) {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.PANEL, Value.FILE)
                .extra(Extra.SNACKBAR, Boolean.toString(fromSnackBar))
                .queue();
    }

    public static void showFileContextMenu() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.MENU, Value.DOWNLOAD).queue();
    }

    public static void historyOpenLink() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.PANEL, Value.LINK).queue();
    }

    public static void historyRemoveLink() {
        TelemetryEvent.create(Category.ACTION, Method.REMOVE, Object.PANEL, Value.LINK).queue();
    }

    public static void showHistoryContextMenu() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.MENU, Value.HISTORY).queue();
    }

    public static void clearHistory() {
        TelemetryEvent.create(Category.ACTION, Method.CLEAR, Object.PANEL, Value.HISTORY).queue();
    }

    public static void openCapture() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.PANEL, Value.CAPTURE).queue();
    }

    public static void openCaptureLink() {
        TelemetryEvent.create(Category.ACTION, Method.OPEN, Object.CAPTURE, Value.LINK).queue();
    }

    public static void editCaptureImage(boolean editAppResolved) {
        TelemetryEvent.create(Category.ACTION, Method.EDIT, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SUCCESS, Boolean.toString(editAppResolved))
                .queue();
    }

    public static void shareCaptureImage(boolean fromSnackBar) {
        TelemetryEvent.create(Category.ACTION, Method.SHARE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SNACKBAR, Boolean.toString(fromSnackBar))
                .queue();
    }

    public static void showCaptureInfo() {
        TelemetryEvent.create(Category.ACTION, Method.SHOW, Object.CAPTURE, Value.INFO).queue();
    }

    public static void deleteCaptureImage() {
        TelemetryEvent.create(Category.ACTION, Method.DELETE, Object.CAPTURE, Value.IMAGE).queue();
    }

}
