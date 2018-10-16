/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Some deprecated class (ie. TelemetryEventPingBuilder) should be replaced, however the replace
// process takes time. To suppress the warning until we are ready to replace.
// Please refer to github issue: #2660
@file:Suppress("DEPRECATION")

package org.mozilla.focus.telemetry

import android.content.Context
import android.os.StrictMode
import android.preference.PreferenceManager
import android.webkit.PermissionRequest
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.provider.ScreenshotContract
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_NEXT
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_PREVIOUS
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.DISMISS
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.DISMISS_BY_BACK
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.DISMISS_BY_CLOSE
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.OPEN_BY_MENU
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.SETTINGS
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.DebugUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.measurement.SettingsMeasurement
import org.mozilla.telemetry.measurement.TelemetryMeasurement
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage
import java.util.HashMap

object TelemetryWrapper {
    internal val TELEMETRY_APP_NAME_ZERDA = "Zerda"

    private val TOOL_BAR_CAPTURE_TELEMETRY_VERSION = 3
    private val RATE_APP_NOTIFICATION_TELEMETRY_VERSION = 2
    private val DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION = 2

    internal object Category {
        val ACTION = "action"
    }

    internal object Method {
        val TYPE_QUERY = "type_query"
        val TYPE_SELECT_QUERY = "select_query"
        val CLICK = "click"
        val CANCEL = "cancel"
        val LONG_PRESS = "long_press"
        val CHANGE = "change"
        val RESET = "reset"
        val CLEAR = "clear"
        val REMOVE = "remove"
        val DELETE = "delete"
        val EDIT = "edit"
        val PERMISSION = "permission"
        val FULLSCREEN = "fullscreen"
        val ADD = "add"
        val SWIPE = "swipe"

        val FOREGROUND = "foreground"
        val BACKGROUND = "background"
        val SHARE = "share"
        val PIN_SHORTCUT = "pin_shortcut"
        val SAVE = "save"
        val COPY = "copy"
        val OPEN = "open"
        val INTENT_URL = "intent_url"
        val TEXT_SELECTION_INTENT = "text_selection_intent"
        val SHOW = "show"
        val LAUNCH = "launch"
    }

    internal object Object {
        val PRIVATE_MODE = "private_mode"
        val PANEL = "panel"
        val TOOLBAR = "toolbar"
        val HOME = "home"
        val CAPTURE = "capture"
        val SEARCH_SUGGESTION = "search_suggestion"
        val SEARCH_BAR = "search_bar"
        val TAB = "tab"
        val TABTRAY = "tab_tray"
        val CLOSE_ALL = "close_all"

        val SETTING = "setting"
        val APP = "app"
        val MENU = "menu"
        val FIND_IN_PAGE = "find_in_page"

        val BROWSER = "browser"
        val BROWSER_CONTEXTMENU = "browser_contextmenu"
        val FIRSTRUN = "firstrun"

        val FEEDBACK = "feedback"
        val DEFAULT_BROWSER = "default_browser"
        val PROMOTE_SHARE = "promote_share"
        val THEMETOY = "themetoy"
        val BANNER = "banner"
        val DOORHANGER = "doorhanger"
    }

    object Value {
        internal val HOME = "home"
        internal val TOPSITE = "top_site"
        internal val DOWNLOAD = "download"
        internal val HISTORY = "history"
        internal val TURBO = "turbo"
        internal val BLOCK_IMAGE = "block_image"
        internal val CLEAR_CACHE = "clear_cache"
        internal val SETTINGS = "settings"

        internal val TABTRAY = "tab_tray"
        internal val TOOLBAR = "toolbar"
        internal val FORWARD = "forward"
        internal val RELOAD = "reload"
        internal val CAPTURE = "capture"
        internal val BOOKMARK = "bookmark"
        internal val FIND_IN_PAGE = "find_in_page"

        internal val SEARCH_BUTTON = "search_btn"
        internal val SEARCH_BOX = "search_box"
        internal val MINI_URLBAR = "mini_urlbar"

        internal val FILE = "file"
        internal val IMAGE = "image"
        internal val LINK = "link"
        internal val FINISH = "finish"
        internal val INFO = "info"

        internal val ENTER = "enter"
        internal val EXIT = "exit"
        internal val GEOLOCATION = "geolocation"
        internal val AUDIO = "audio"
        internal val VIDEO = "video"
        internal val MIDI = "midi"
        internal val EME = "eme"

        internal val LEARN_MORE = "learn_more"

        const val DISMISS = "dismiss"
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
        const val SHARE = "share"
        const val SUGGESTION = "sugestion"

        internal val LAUNCHER = "launcher"
        internal val EXTERNAL_APP = "external_app"
        internal val SHORTCUT = "shortcut"

        internal val BACKGROUND = "background"
        internal val ITEM = "item"
        internal val PAGE = "page"
        internal val WIFI_FINDER = "wifi_finder"
        internal val VPN = "vpn"

        internal val PREVIOUS = "previous"
        internal val NEXT = "next"

        internal val NIGHT_MODE = "night_mode"
        internal val NIGHT_MODE_BRIGHTNESS = "night_mode_brightness"
    }

    internal object Extra {
        val TO = "to"
        val ON = "on"
        val DEFAULT = "default"
        val SUCCESS = "success"
        val SNACKBAR = "snackbar"
        val SOURCE = "source"
        val VERSION = "version"
        val TYPE = "type"
        val DIRECTION = "direction"
        val CATEGORY = "category"
        val CATEGORY_VERSION = "category_version"
    }

    object Extra_Value {
        const val SETTING = "settings"
        const val CONTEXTUAL_HINTS = "contextual_hints"
        const val NOTIFICATION = "notification"
        internal val WEB_SEARCH = "web_search"
        internal val TEXT_SELECTION = "text_selection"
        internal val DEFAULT = "default"
    }

    enum class FIND_IN_PAGE {
        OPEN_BY_MENU,
        CLICK_PREVIOUS,
        CLICK_NEXT,
        DISMISS_BY_CLOSE,
        DISMISS_BY_BACK,
        DISMISS
    }

    @JvmStatic
    fun isTelemetryEnabled(context: Context): Boolean {
        return Inject.isTelemetryEnabled(context)
    }

    @JvmStatic
    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        preferences.edit()
                .putBoolean(resources.getString(R.string.pref_key_telemetry), enabled)
                .apply()

        TelemetryHolder.get()
                .configuration
                .setUploadEnabled(enabled).isCollectionEnabled = enabled
    }

    fun init(context: Context) {
        // When initializing the telemetry library it will make sure that all directories exist and
        // are readable/writable.
        val threadPolicy = StrictMode.allowThreadDiskWrites()
        try {
            val resources = context.resources

            val telemetryEnabled = isTelemetryEnabled(context)

            updateDefaultBrowserStatus(context)
            updatePrefValue(context, resources.getString(R.string.pref_key_webview_version), DebugUtils.loadWebViewVersion(context))

            val configuration = TelemetryConfiguration(context)
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
                    .setSettingsProvider(CustomSettingsProvider())
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(telemetryEnabled)

            val serializer = JSONPingSerializer()
            val storage = FileTelemetryStorage(configuration, serializer)
            val client = HttpURLConnectionTelemetryClient()
            val scheduler = JobSchedulerTelemetryScheduler()

            TelemetryHolder.set(Telemetry(configuration, storage, client, scheduler)
                    .addPingBuilder(TelemetryCorePingBuilder(configuration))
                    .addPingBuilder(TelemetryEventPingBuilder(configuration))
                    .setDefaultSearchProvider(createDefaultSearchProvider(context)))
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    private fun updateDefaultBrowserStatus(context: Context) {
        Settings.updatePrefDefaultBrowserIfNeeded(context, Browsers.isDefaultBrowser(context))
    }

    private fun updatePrefValue(context: Context, key: String, value: String) {
        Settings.updatePrefString(context, key, value)
    }

    private fun createDefaultSearchProvider(context: Context): DefaultSearchMeasurement.DefaultSearchEngineProvider {
        return DefaultSearchMeasurement.DefaultSearchEngineProvider {
            SearchEngineManager.getInstance()
                    .getDefaultSearchEngine(context)
                    .identifier
        }
    }

    @JvmStatic
    fun toggleFirstRunPageEvent(enableTurboMode: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.FIRSTRUN, Value.TURBO)
                .extra(Extra.TO, java.lang.Boolean.toString(enableTurboMode))
                .queue()
    }

    @JvmStatic
    fun finishFirstRunEvent(duration: Long) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN, Value.FINISH)
                .extra(Extra.ON, java.lang.Long.toString(duration))
                .queue()
    }

    @JvmStatic
    fun browseIntentEvent() {
        EventBuilder(Category.ACTION, Method.INTENT_URL, Object.APP).queue()
    }

    @JvmStatic
    fun textSelectionIntentEvent() {
        EventBuilder(Category.ACTION, Method.TEXT_SELECTION_INTENT, Object.APP).queue()
    }

    @JvmStatic
    fun launchByAppLauncherEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.LAUNCHER).queue()
    }

    @JvmStatic
    fun launchByHomeScreenShortcutEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.SHORTCUT).queue()
    }

    @JvmStatic
    fun launchByTextSelectionSearchEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                .extra(Extra.TYPE, Extra_Value.TEXT_SELECTION)
                .queue()
    }

    @JvmStatic
    fun launchByWebSearchEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                .extra(Extra.TYPE, Extra_Value.WEB_SEARCH)
                .queue()
    }

    @JvmStatic
    fun launchByExternalAppEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP).queue()
    }

    @JvmStatic
    fun settingsEvent(key: String, value: String) {
        // We only log whitelist-ed setting
        val validPrefKey = FirebaseEvent.getValidPrefKey(key)
        if (validPrefKey != null) {
            EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, validPrefKey)
                    .extra(Extra.TO, value)
                    .queue()
        }

    }

    @JvmStatic
    fun settingsClickEvent(key: String) {
        val validPrefKey = FirebaseEvent.getValidPrefKey(key)
        if (validPrefKey != null) {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, validPrefKey).queue()
        }
    }

    @JvmStatic
    fun settingsLearnMoreClickEvent(source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, Value.LEARN_MORE)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @JvmStatic
    fun settingsLocaleChangeEvent(key: String, value: String, isDefault: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .extra(Extra.DEFAULT, java.lang.Boolean.toString(isDefault))
                .queue()
    }

    @JvmStatic
    fun startSession() {
        TelemetryHolder.get().recordSessionStart()

        EventBuilder(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    @JvmStatic
    fun stopSession() {
        TelemetryHolder.get().recordSessionEnd()

        EventBuilder(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
    }

    @JvmStatic
    fun stopMainActivity() {
        TelemetryHolder.get()
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryEventPingBuilder.TYPE)
                .scheduleUpload()
    }

    @JvmStatic
    fun openWebContextMenuEvent() {
        EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.BROWSER).queue()
    }

    @JvmStatic
    fun cancelWebContextMenuEvent() {
        EventBuilder(Category.ACTION, Method.CANCEL, Object.BROWSER_CONTEXTMENU).queue()
    }

    @JvmStatic
    fun shareLinkEvent() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @JvmStatic
    fun shareImageEvent() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun saveImageEvent() {
        EventBuilder(Category.ACTION, Method.SAVE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun copyLinkEvent() {
        EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @JvmStatic
    fun copyImageEvent() {
        EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @JvmStatic
    fun addNewTabFromContextMenu() {
        EventBuilder(Category.ACTION, Method.ADD, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @JvmStatic
    fun browseGeoLocationPermissionEvent() {
        EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.GEOLOCATION).queue()
    }

    @JvmStatic
    fun browseFilePermissionEvent() {
        EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.FILE).queue()
    }

    @JvmStatic
    fun browsePermissionEvent(requests: Array<String>) {
        for (request in requests) {
            val value: String
            when (request) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> value = Value.AUDIO
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> value = Value.VIDEO
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> value = Value.EME
                PermissionRequest.RESOURCE_MIDI_SYSEX -> value = Value.MIDI
                else -> value = request
            }
            EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, value).queue()
        }
    }

    @JvmStatic
    fun browseEnterFullScreenEvent() {
        EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.ENTER).queue()
    }

    @JvmStatic
    fun browseExitFullScreenEvent() {
        EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.EXIT).queue()
    }

    @JvmStatic
    fun showMenuHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HOME).queue()
    }

    @JvmStatic
    fun showTabTrayHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.HOME).queue()
    }

    @JvmStatic
    fun showTabTrayToolbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.TOOLBAR).queue()
    }

    @JvmStatic
    fun showMenuToolbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.TOOLBAR).queue()
    }

    @JvmStatic
    fun clickMenuDownload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.DOWNLOAD).queue()
    }

    @JvmStatic
    fun clickMenuHistory() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.HISTORY).queue()
    }

    @JvmStatic
    fun clickMenuCapture() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CAPTURE).queue()
    }

    @JvmStatic
    fun showPanelBookmark() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun showPanelDownload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.DOWNLOAD).queue()
    }

    @JvmStatic
    fun showPanelHistory() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.HISTORY).queue()
    }

    @JvmStatic
    fun showPanelCapture() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.CAPTURE).queue()
    }

    @JvmStatic
    fun menuTurboChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.TURBO)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @JvmStatic
    fun menuNightModeChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.NIGHT_MODE)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @JvmStatic
    fun menuBlockImageChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.BLOCK_IMAGE)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @JvmStatic
    fun clickMenuClearCache() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CLEAR_CACHE).queue()
    }

    @JvmStatic
    fun clickMenuSettings() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, SETTINGS).queue()
    }

    @JvmStatic
    fun clickMenuExit() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.EXIT).queue()
    }

    @JvmStatic
    fun clickMenuBookmark() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun clickToolbarForward() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.FORWARD).queue()
    }

    @JvmStatic
    fun clickToolbarReload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD).queue()
    }

    @JvmStatic
    fun clickToolbarShare() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK).queue()
    }

    @JvmStatic
    fun clickToolbarBookmark(isAdd: Boolean) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.BOOKMARK)
                .extra(Extra.TO, java.lang.Boolean.toString(isAdd))
                .queue()
    }

    @JvmStatic
    fun clickAddToHome() {
        EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.TOOLBAR, Value.LINK).queue()
    }

    @JvmStatic
    fun clickToolbarCapture(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.CAPTURE)
                .extra(Extra.VERSION, Integer.toString(TOOL_BAR_CAPTURE_TELEMETRY_VERSION))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun clickTopSiteOn(index: Int) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, Integer.toString(index))
                .queue()

        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOPSITE)
                .queue()
    }

    @JvmStatic
    fun removeTopSite(isDefault: Boolean) {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.HOME, Value.LINK)
                .extra(Extra.DEFAULT, java.lang.Boolean.toString(isDefault))
                //  TODO: add index
                .queue()
    }

    @JvmStatic
    fun addNewTabFromHome() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.HOME).queue()
    }

    @JvmStatic
    fun urlBarEvent(isUrl: Boolean, isSuggestion: Boolean) {
        if (isUrl) {
            TelemetryWrapper.browseEvent()
        } else if (isSuggestion) {
            TelemetryWrapper.searchSelectEvent()
        } else {
            TelemetryWrapper.searchEnterEvent()
        }
    }

    private fun browseEvent() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK).queue()
    }

    @JvmStatic
    fun searchSelectEvent() {
        val telemetry = TelemetryHolder.get()

        EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_SUGGESTION, searchEngine.identifier)
    }

    private fun searchEnterEvent() {
        val telemetry = TelemetryHolder.get()

        EventBuilder(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

    @JvmStatic
    fun togglePrivateMode(enter: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.PRIVATE_MODE, if (enter) Value.ENTER else Value.EXIT).queue()
    }

    @JvmStatic
    fun searchSuggestionLongClick() {
        EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.SEARCH_SUGGESTION).queue()
    }

    @JvmStatic
    fun searchClear() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.SEARCH_BAR).queue()
    }

    @JvmStatic
    fun searchDismiss() {
        EventBuilder(Category.ACTION, Method.CANCEL, Object.SEARCH_BAR).queue()
    }

    @JvmStatic
    fun showSearchBarHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BOX).queue()
    }

    @JvmStatic
    fun clickUrlbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.MINI_URLBAR).queue()
    }

    @JvmStatic
    fun clickToolbarSearch() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON).queue()
    }

    @JvmStatic
    fun clickAddTabToolbar() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOOLBAR).queue()
    }

    @JvmStatic
    fun clickAddTabTray() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TABTRAY).queue()
    }

    @JvmStatic
    fun clickTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.TAB, Value.TABTRAY).queue()
    }

    @JvmStatic
    fun closeTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.TAB, Value.TABTRAY).queue()
    }

    @JvmStatic
    fun swipeTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.TAB, Value.TABTRAY).queue()
    }

    @JvmStatic
    fun closeAllTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CLOSE_ALL, Value.TABTRAY).queue()
    }

    @JvmStatic
    fun downloadRemoveFile() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.FILE).queue()
    }

    @JvmStatic
    fun downloadDeleteFile() {
        EventBuilder(Category.ACTION, Method.DELETE, Object.PANEL, Value.FILE).queue()
    }

    @JvmStatic
    fun downloadOpenFile(fromSnackBar: Boolean) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.FILE)
                .extra(Extra.SNACKBAR, java.lang.Boolean.toString(fromSnackBar))
                .queue()
    }

    @JvmStatic
    fun showFileContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.DOWNLOAD).queue()
    }

    @JvmStatic
    fun historyOpenLink() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LINK).queue()
    }

    @JvmStatic
    fun historyRemoveLink() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.LINK).queue()
    }

    @JvmStatic
    fun bookmarkRemoveItem() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun bookmarkEditItem() {
        EventBuilder(Category.ACTION, Method.EDIT, Object.PANEL, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun bookmarkOpenItem() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun showHistoryContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HISTORY).queue()
    }

    @JvmStatic
    fun showBookmarkContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.BOOKMARK).queue()
    }

    @JvmStatic
    fun clearHistory() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.PANEL, Value.HISTORY).queue()
    }

    @JvmStatic
    fun openCapture() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.CAPTURE).queue()
    }

    @JvmStatic
    fun openCaptureLink(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.CAPTURE, Value.LINK)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun editCaptureImage(editAppResolved: Boolean, category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.EDIT, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SUCCESS, java.lang.Boolean.toString(editAppResolved))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun shareCaptureImage(fromSnackBar: Boolean, category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SNACKBAR, java.lang.Boolean.toString(fromSnackBar))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun showCaptureInfo(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.CAPTURE, Value.INFO)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun deleteCaptureImage(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.DELETE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @JvmStatic
    fun feedbackClickEvent(value: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @JvmStatic
    fun showFeedbackDialog() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK).queue()
    }

    @JvmStatic
    fun showRateAppNotification() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    // TODO: test Context from contetReceiver
    @JvmStatic
    fun clickRateAppNotification(value: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @JvmStatic
    fun clickRateAppNotification() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, RATE_APP_NOTIFICATION_TELEMETRY_VERSION.toString())
                .queue()
    }

    @JvmStatic
    fun showDefaultSettingNotification() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @JvmStatic
    fun clickDefaultSettingNotification() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, Integer.toString(DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION))
                .queue()
    }

    @JvmStatic
    fun onDefaultBrowserServiceFailed() {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.DEFAULT_BROWSER)
                .extra(Extra.SUCCESS, java.lang.Boolean.toString(false))
                .queue()
    }

    @JvmStatic
    fun promoteShareClickEvent(value: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PROMOTE_SHARE, value)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @JvmStatic
    fun showPromoteShareDialog() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.PROMOTE_SHARE).queue()
    }

    @JvmStatic
    fun changeThemeTo(themeName: String) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.THEMETOY)
                .extra(Extra.TO, themeName)
                .queue()
    }

    @JvmStatic
    fun resetThemeToDefault() {
        EventBuilder(Category.ACTION, Method.RESET, Object.THEMETOY)
                .extra(Extra.TO, Extra_Value.DEFAULT)
                .queue()
    }

    @JvmStatic
    fun erasePrivateModeNotification() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.PRIVATE_MODE)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @JvmStatic
    fun clickBannerBackground(pageId: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.BANNER, Value.BACKGROUND)
                .extra(Extra.SOURCE, pageId)
                .queue()
    }

    @JvmStatic
    fun clickBannerItem(pageId: String, itemPosition: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.BANNER, Value.ITEM)
                .extra(Extra.SOURCE, pageId)
                .extra(Extra.ON, Integer.toString(itemPosition))
                .queue()
    }

    @JvmStatic
    fun swipeBannerItem(directionX: Int, toItemPosition: Int) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.BANNER, Value.PAGE)
                .extra(Extra.DIRECTION, Integer.toString(directionX))
                .extra(Extra.TO, Integer.toString(toItemPosition))
                .queue()
    }

    @JvmStatic
    fun clickWifiFinderSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.HOME, Value.WIFI_FINDER)
                .queue()
    }

    @JvmStatic
    fun clickWifiFinderSurveyFeedback(positive: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, if (positive) Value.POSITIVE else Value.NEGATIVE)
                .extra(Extra.SOURCE, Value.WIFI_FINDER)
                .queue()
    }

    @JvmStatic
    fun dismissWifiFinderSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, Value.DISMISS)
                .extra(Extra.SOURCE, Value.WIFI_FINDER)
                .queue()
    }

    @JvmStatic
    fun clickVpnSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.HOME, Value.VPN)
                .queue()
    }

    @JvmStatic
    fun clickVpnSurveyFeedback(positive: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, if (positive) Value.POSITIVE else Value.NEGATIVE)
                .extra(Extra.SOURCE, Value.VPN)
                .queue()
    }

    @JvmStatic
    fun dismissVpnSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, Value.DISMISS)
                .extra(Extra.SOURCE, Value.VPN)
                .queue()
    }

    @JvmStatic
    fun findInPage(type: FIND_IN_PAGE) {
        val builder = when (type) {
            OPEN_BY_MENU -> EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.FIND_IN_PAGE)
            CLICK_PREVIOUS -> EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.PREVIOUS)
            CLICK_NEXT -> EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.NEXT)
            DISMISS_BY_BACK -> EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
                    .extra(Extra.SOURCE, "back_button")
            DISMISS_BY_CLOSE -> EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
                    .extra(Extra.SOURCE, "close_button")
            DISMISS -> EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
                    .extra(Extra.SOURCE, "other")
        }

        builder.queue()
    }

    @JvmStatic
    fun nightModeBrightnessChangeTo(value: Int, fromSetting: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, Value.NIGHT_MODE_BRIGHTNESS)
                .extra(Extra.SOURCE, if (fromSetting) Object.SETTING else Object.MENU)
                .extra(Extra.TO, value.toString())
                .queue()
    }

    internal class EventBuilder @JvmOverloads constructor(category: String, method: String, `object`: String?, value: String? = null) {
        var telemetryEvent: TelemetryEvent
        var firebaseEvent: FirebaseEvent

        init {
            lazyInit()
            telemetryEvent = TelemetryEvent.create(category, method, `object`, value)
            firebaseEvent = FirebaseEvent.create(category, method, `object`, value)
        }

        fun extra(key: String, value: String): EventBuilder {
            telemetryEvent.extra(key, value)
            firebaseEvent.param(key, value)
            return this
        }

        fun queue() {

            val context = TelemetryHolder.get().configuration.context
            if (context != null) {
                telemetryEvent.queue()
                firebaseEvent.event(context)
            }
        }

        companion object {

            fun lazyInit() {

                if (FirebaseEvent.isInitialized()) {
                    return
                }
                val context = TelemetryHolder.get().configuration.context ?: return
                val prefKeyWhitelist = HashMap<String, String>()
                prefKeyWhitelist[context.getString(R.string.pref_key_search_engine)] = "search_engine"

                prefKeyWhitelist[context.getString(R.string.pref_key_privacy_block_ads)] = "privacy_ads"
                prefKeyWhitelist[context.getString(R.string.pref_key_privacy_block_analytics)] = "privacy_analytics"
                prefKeyWhitelist[context.getString(R.string.pref_key_privacy_block_social)] = "privacy_social"
                prefKeyWhitelist[context.getString(R.string.pref_key_privacy_block_other)] = "privacy_other"
                prefKeyWhitelist[context.getString(R.string.pref_key_turbo_mode)] = "turbo_mode"

                prefKeyWhitelist[context.getString(R.string.pref_key_performance_block_webfonts)] = "block_webfonts"
                prefKeyWhitelist[context.getString(R.string.pref_key_performance_block_images)] = "block_images"

                prefKeyWhitelist[context.getString(R.string.pref_key_default_browser)] = "default_browser"

                prefKeyWhitelist[context.getString(R.string.pref_key_telemetry)] = "telemetry"

                prefKeyWhitelist[context.getString(R.string.pref_key_give_feedback)] = "give_feedback"
                prefKeyWhitelist[context.getString(R.string.pref_key_share_with_friends)] = "share_with_friends"
                prefKeyWhitelist[context.getString(R.string.pref_key_about)] = "key_about"
                prefKeyWhitelist[context.getString(R.string.pref_key_help)] = "help"
                prefKeyWhitelist[context.getString(R.string.pref_key_rights)] = "rights"

                prefKeyWhitelist[context.getString(R.string.pref_key_webview_version)] = "webview_version"
                // data saving
                prefKeyWhitelist[context.getString(R.string.pref_key_data_saving_block_ads)] = "saving_block_ads"
                prefKeyWhitelist[context.getString(R.string.pref_key_data_saving_block_webfonts)] = "data_webfont"
                prefKeyWhitelist[context.getString(R.string.pref_key_data_saving_block_images)] = "data_images"
                prefKeyWhitelist[context.getString(R.string.pref_key_data_saving_block_tab_restore)] = "tab_restore"

                // storage and cache
                prefKeyWhitelist[context.getString(R.string.pref_key_storage_clear_browsing_data)] = "clear_browsing_data)"
                prefKeyWhitelist[context.getString(R.string.pref_key_removable_storage_available_on_create)] = "remove_storage"
                prefKeyWhitelist[context.getString(R.string.pref_key_storage_save_downloads_to)] = "save_downloads_to"
                prefKeyWhitelist[context.getString(R.string.pref_key_showed_storage_message)] = "storage_message)"

                // rate / share app already have telemetry

                // clear browsing data
                prefKeyWhitelist[context.getString(R.string.pref_value_clear_browsing_history)] = "clear_browsing_his"
                prefKeyWhitelist[context.getString(R.string.pref_value_clear_form_history)] = "clear_form_his"
                prefKeyWhitelist[context.getString(R.string.pref_value_clear_cookies)] = "clear_cookies"
                prefKeyWhitelist[context.getString(R.string.pref_value_clear_cache)] = "clear_cache"

                // data saving path values
                prefKeyWhitelist[context.getString(R.string.pref_value_saving_path_sd_card)] = "path_sd_card"
                prefKeyWhitelist[context.getString(R.string.pref_value_saving_path_internal_storage)] = "path_internal_storage"

                //  default browser already have telemetry

                // NewFeatureNotice already have telemetry

                FirebaseEvent.setPrefKeyWhitelist(prefKeyWhitelist)

            }
        }
    }

    private class CustomSettingsProvider : SettingsMeasurement.SharedPreferenceSettingsProvider() {

        private val custom = HashMap<String, Any>(2)

        override fun update(configuration: TelemetryConfiguration) {
            super.update(configuration)

            val context = configuration.context
            addCustomPing(configuration, ThemeToyMeasurement(context))
            addCustomPing(configuration, CaptureCountMeasurement(context))
        }


        internal fun addCustomPing(configuration: TelemetryConfiguration, measurement: TelemetryMeasurement) {
            var preferenceKeys: MutableSet<String>? = configuration.preferencesImportantForTelemetry
            if (preferenceKeys == null) {
                configuration.setPreferencesImportantForTelemetry(*arrayOf())
                preferenceKeys = configuration.preferencesImportantForTelemetry
            }
            preferenceKeys!!.add(measurement.fieldName)
            custom[measurement.fieldName] = measurement.flush()
        }

        override fun containsKey(key: String): Boolean {
            return super.containsKey(key) or custom.containsKey(key)
        }

        override fun getValue(key: String): Any {
            val value = custom[key]

            return value ?: super.getValue(key)
        }
    }

    private class ThemeToyMeasurement internal constructor(internal var context: Context) : TelemetryMeasurement(MEASUREMENT_CURRENT_THEME) {

        override fun flush(): Any {
            return ThemeManager.getCurrentThemeName(context)
        }

        companion object {

            private val MEASUREMENT_CURRENT_THEME = "current_theme"
        }
    }

    private class CaptureCountMeasurement internal constructor(private val context: Context) : TelemetryMeasurement(MEASUREMENT_CAPTURE_COUNT) {

        override fun flush(): Any {
            var captureCount: Int = -1
            if ("main" == Thread.currentThread().name) {
                throw RuntimeException("Call from main thread exception")
            }
            try {
                context.contentResolver.query(ScreenshotContract.Screenshot.CONTENT_URI, null, null, null, null)!!.use { cursor ->
                    captureCount = cursor.count
                }
            } catch (e: Exception) {
                captureCount = -1
            }

            return captureCount
        }

        companion object {
            private val MEASUREMENT_CAPTURE_COUNT = "capture_count"
        }
    }
}
