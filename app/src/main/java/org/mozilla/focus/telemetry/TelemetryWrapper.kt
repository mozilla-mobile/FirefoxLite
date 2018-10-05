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
import org.mozilla.telemetry.annotation.TelemetryDoc
import org.mozilla.telemetry.annotation.TelemetryExtra
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
    private val FIND_IN_PAGE_VERSION = 2
    private const val SEARCHCLEAR_TELEMETRY_VERSION = "2"
    private const val SEARCHDISMISS_TELEMETRY_VERSION = "2"

    internal object Category {
        const val ACTION = "action"
    }

    internal object Method {
        const val TYPE_QUERY = "type_query"
        const val TYPE_SELECT_QUERY = "select_query"
        const val CLICK = "click"
        const val CANCEL = "cancel"
        const val LONG_PRESS = "long_press"
        const val CHANGE = "change"
        const val RESET = "reset"
        const val CLEAR = "clear"
        const val REMOVE = "remove"
        const val DELETE = "delete"
        const val EDIT = "edit"
        const val PERMISSION = "permission"
        const val FULLSCREEN = "fullscreen"
        const val ADD = "add"
        const val SWIPE = "swipe"

        const val FOREGROUND = "foreground"
        const val BACKGROUND = "background"
        const val SHARE = "share"
        const val PIN_SHORTCUT = "pin_shortcut"
        const val SAVE = "save"
        const val COPY = "copy"
        const val OPEN = "open"
        const val SHOW = "show"
        const val LAUNCH = "launch"
    }

    internal object Object {
        const val PRIVATE_MODE = "private_mode"
        const val PANEL = "panel"
        const val TOOLBAR = "toolbar"
        const val HOME = "home"
        const val CAPTURE = "capture"
        const val SEARCH_SUGGESTION = "search_suggestion"
        const val SEARCH_BAR = "search_bar"
        const val TAB = "tab"
        const val TABTRAY = "tab_tray"
        const val CLOSE_ALL = "close_all"

        const val SETTING = "setting"
        const val APP = "app"
        const val MENU = "menu"
        const val FIND_IN_PAGE = "find_in_page"

        const val BROWSER = "browser"
        const val BROWSER_CONTEXTMENU = "browser_contextmenu"
        const val FIRSTRUN = "firstrun"

        const val FEEDBACK = "feedback"
        const val DEFAULT_BROWSER = "default_browser"
        const val PROMOTE_SHARE = "promote_share"
        const val THEMETOY = "themetoy"
        const val BANNER = "banner"
        const val DOORHANGER = "doorhanger"
    }

    object Value {
        internal const val HOME = "home"
        internal const val TOPSITE = "top_site"
        internal const val DOWNLOAD = "download"
        internal const val HISTORY = "history"
        internal const val TURBO = "turbo"
        internal const val BLOCK_IMAGE = "block_image"
        internal const val CLEAR_CACHE = "clear_cache"
        internal const val SETTINGS = "settings"

        internal const val TABTRAY = "tab_tray"
        internal const val TOOLBAR = "toolbar"
        internal const val FORWARD = "forward"
        internal const val RELOAD = "reload"
        internal const val CAPTURE = "capture"
        internal const val BOOKMARK = "bookmark"
        internal const val FIND_IN_PAGE = "find_in_page"

        internal const val SEARCH_BUTTON = "search_btn"
        internal const val SEARCH_BOX = "search_box"
        internal const val MINI_URLBAR = "mini_urlbar"

        internal const val FILE = "file"
        internal const val IMAGE = "image"
        internal const val LINK = "link"
        internal const val FINISH = "finish"
        internal const val INFO = "info"

        internal const val ENTER = "enter"
        internal const val EXIT = "exit"
        internal const val GEOLOCATION = "geolocation"
        internal const val AUDIO = "audio"
        internal const val VIDEO = "video"
        internal const val MIDI = "midi"
        internal const val EME = "eme"

        internal const val LEARN_MORE = "learn_more"

        const val DISMISS = "dismiss"
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
        const val SHARE = "share"
        const val SUGGESTION = "sugestion"

        internal const val LAUNCHER = "launcher"
        internal const val EXTERNAL_APP = "external_app"
        internal const val SHORTCUT = "shortcut"

        internal const val BACKGROUND = "background"
        internal const val ITEM = "item"
        internal const val PAGE = "page"
        internal const val WIFI_FINDER = "wifi_finder"
        internal const val VPN = "vpn"


        internal const val PREVIOUS = "previous"
        internal const val NEXT = "next"

        internal const val NIGHT_MODE = "night_mode"
        internal const val NIGHT_MODE_BRIGHTNESS = "night_mode_brightness"
    }

    internal object Extra {
        const val TO = "to"
        const val ON = "on"
        const val DEFAULT = "default"
        const val SUCCESS = "success"
        const val SNACKBAR = "snackbar"
        const val SOURCE = "source"
        const val VERSION = "version"
        const val TYPE = "type"
        const val DIRECTION = "direction"
        const val CATEGORY = "category"
        // Remove the last character cause Telemetry library will do that for you.( > 15chars)
        const val CATEGORY_VERSION = "category_versio"
    }

    object Extra_Value {
        const val SETTING = "settings"
        const val CONTEXTUAL_HINTS = "contextual_hints"
        const val NOTIFICATION = "notification"
        internal const val WEB_SEARCH = "web_search"
        internal const val TEXT_SELECTION = "text_selection"
        internal const val DEFAULT = "default"
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


    @TelemetryDoc(
            name = "Turn on Turbo Mode in First Run",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.FIRSTRUN,
            value = Value.TURBO,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
    @JvmStatic
    fun toggleFirstRunPageEvent(enableTurboMode: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.FIRSTRUN, Value.TURBO)
                .extra(Extra.TO, java.lang.Boolean.toString(enableTurboMode))
                .queue()
    }

    @TelemetryDoc(
            name = "Finish First Run",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FIRSTRUN,
            value = Value.FINISH,
            extras = [TelemetryExtra(name = Extra.ON, value = "time spent on First Run")])
    @JvmStatic
    fun finishFirstRunEvent(duration: Long) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN, Value.FINISH)
                .extra(Extra.ON, java.lang.Long.toString(duration))
                .queue()
    }

    @TelemetryDoc(
            name = "App is launched by Launcher",
            action = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.LAUNCHER,
            extras = [])
    @JvmStatic
    fun launchByAppLauncherEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.LAUNCHER).queue()
    }

    @TelemetryDoc(
            name = "App is launched by Shortcut",
            action = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.SHORTCUT,
            extras = [])
    @JvmStatic
    fun launchByHomeScreenShortcutEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.SHORTCUT).queue()
    }

    @TelemetryDoc(
            name = "App is launched by text_selection",
            action = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.EXTERNAL_APP,
            extras = [TelemetryExtra(name = Extra.TYPE, value = Extra_Value.TEXT_SELECTION)])
    @JvmStatic
    fun launchByTextSelectionSearchEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                .extra(Extra.TYPE, Extra_Value.TEXT_SELECTION)
                .queue()
    }

    @TelemetryDoc(
            name = "App is launched by Web Search",
            action = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.EXTERNAL_APP,
            extras = [TelemetryExtra(name = Extra.TYPE, value = Extra_Value.WEB_SEARCH)])
    @JvmStatic
    fun launchByWebSearchEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                .extra(Extra.TYPE, Extra_Value.WEB_SEARCH)
                .queue()
    }

    @TelemetryDoc(
            name = "App is Launched by other Apps",
            action = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.EXTERNAL_APP,
            extras = [])
    @JvmStatic
    fun launchByExternalAppEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP).queue()
    }

    @TelemetryDoc(
            name = "Users changed a Setting",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.SETTING,
            value = "settings pref key",
            extras = [TelemetryExtra(name = Extra.TO, value = "New Value for the pref")])
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

    @TelemetryDoc(
            name = "Users clicked on a Setting",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SETTING,
            value = "settings pref key",
            extras = [])
    @JvmStatic
    fun settingsClickEvent(key: String) {
        val validPrefKey = FirebaseEvent.getValidPrefKey(key)
        if (validPrefKey != null) {
            EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, validPrefKey).queue()
        }
    }

    @TelemetryDoc(
            name = "Users clicked on the Learn More link in Settings",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SETTING,
            value = Value.LEARN_MORE,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "TurboMode,Telemetry")])
    @JvmStatic
    fun settingsLearnMoreClickEvent(source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, Value.LEARN_MORE)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Users clicked on the Learn More link in Settings",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.SETTING,
            value = "pref_locale",
            extras = [TelemetryExtra(name = Extra.TO, value = "Locale "),
                TelemetryExtra(name = Extra.DEFAULT, value = "true,false")])
    @JvmStatic
    fun settingsLocaleChangeEvent(key: String, value: String, isDefault: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, key)
                .extra(Extra.TO, value)
                .extra(Extra.DEFAULT, java.lang.Boolean.toString(isDefault))
                .queue()
    }

    @TelemetryDoc(
            name = "Session starts",
            action = Category.ACTION,
            method = Method.FOREGROUND,
            `object` = Object.APP,
            value = "",
            extras = [])
    @JvmStatic
    fun startSession() {
        TelemetryHolder.get().recordSessionStart()

        EventBuilder(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    @TelemetryDoc(
            name = "Session ends",
            action = Category.ACTION,
            method = Method.BACKGROUND,
            `object` = Object.APP,
            value = "",
            extras = [])
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

    @TelemetryDoc(
            name = "Long Press ContextMenu",
            action = Category.ACTION,
            method = Method.LONG_PRESS,
            `object` = Object.BROWSER,
            value = "",
            extras = [])
    @JvmStatic
    fun openWebContextMenuEvent() {
        EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.BROWSER).queue()
    }

    @TelemetryDoc(
            name = "Cancel ContextMenu",
            action = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = "",
            extras = [])
    @JvmStatic
    fun cancelWebContextMenuEvent() {
        EventBuilder(Category.ACTION, Method.CANCEL, Object.BROWSER_CONTEXTMENU).queue()
    }

    @TelemetryDoc(
            name = "Share link via ContextMenu",
            action = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun shareLinkEvent() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Share image via ContextMenu",
            action = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.IMAGE,
            extras = [])
    @JvmStatic
    fun shareImageEvent() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @TelemetryDoc(
            name = "Save image via ContextMenu",
            action = Category.ACTION,
            method = Method.SAVE,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.IMAGE,
            extras = [])
    @JvmStatic
    fun saveImageEvent() {
        EventBuilder(Category.ACTION, Method.SAVE, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @TelemetryDoc(
            name = "Copy link via ContextMenu",
            action = Category.ACTION,
            method = Method.COPY,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun copyLinkEvent() {
        EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Copy image via ContextMenu",
            action = Category.ACTION,
            method = Method.COPY,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.IMAGE,
            extras = [])
    @JvmStatic
    fun copyImageEvent() {
        EventBuilder(Category.ACTION, Method.COPY, Object.BROWSER_CONTEXTMENU, Value.IMAGE).queue()
    }

    @TelemetryDoc(
            name = "Add link via ContextMenu",
            action = Category.ACTION,
            method = Method.ADD,
            `object` = Object.BROWSER_CONTEXTMENU,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun addNewTabFromContextMenu() {
        EventBuilder(Category.ACTION, Method.ADD, Object.BROWSER_CONTEXTMENU, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Permission-Geolocation",
            action = Category.ACTION,
            method = Method.PERMISSION,
            `object` = Object.BROWSER,
            value = Value.GEOLOCATION,
            extras = [])
    @JvmStatic
    fun browseGeoLocationPermissionEvent() {
        EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.GEOLOCATION).queue()
    }

    @TelemetryDoc(
            name = "Permission-File",
            action = Category.ACTION,
            method = Method.PERMISSION,
            `object` = Object.BROWSER,
            value = Value.FILE,
            extras = [])
    @JvmStatic
    fun browseFilePermissionEvent() {
        EventBuilder(Category.ACTION, Method.PERMISSION, Object.BROWSER, Value.FILE).queue()
    }

    @TelemetryDoc(
            name = "Permission-Media",
            action = Category.ACTION,
            method = Method.PERMISSION,
            `object` = Object.BROWSER,
            value = "${Value.AUDIO},${Value.VIDEO},${Value.EME},${Value.MIDI}",
            extras = [])
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

    @TelemetryDoc(
            name = "Enter full screen",
            action = Category.ACTION,
            method = Method.FULLSCREEN,
            `object` = Object.BROWSER,
            value = Value.ENTER,
            extras = [])
    @JvmStatic
    fun browseEnterFullScreenEvent() {
        EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.ENTER).queue()
    }

    @TelemetryDoc(
            name = "Exit full screen",
            action = Category.ACTION,
            method = Method.FULLSCREEN,
            `object` = Object.BROWSER,
            value = Value.EXIT,
            extras = [])
    @JvmStatic
    fun browseExitFullScreenEvent() {
        EventBuilder(Category.ACTION, Method.FULLSCREEN, Object.BROWSER, Value.EXIT).queue()
    }

    @TelemetryDoc(
            name = "Show Menu from Home",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.HOME,
            extras = [])
    @JvmStatic
    fun showMenuHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HOME).queue()
    }

    @TelemetryDoc(
            name = "Show TabTray from Home",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.TABTRAY,
            value = Value.HOME,
            extras = [])
    @JvmStatic
    fun showTabTrayHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.HOME).queue()
    }

    @TelemetryDoc(
            name = "Show TabTray from Toolbar",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.TABTRAY,
            value = Value.TOOLBAR,
            extras = [])
    @JvmStatic
    fun showTabTrayToolbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.TOOLBAR).queue()
    }

    @TelemetryDoc(
            name = "Show Menu from Toolbar",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.TOOLBAR,
            extras = [])
    @JvmStatic
    fun showMenuToolbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.TOOLBAR).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Downloads",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.DOWNLOAD,
            extras = [])
    @JvmStatic
    fun clickMenuDownload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.DOWNLOAD).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - History",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.HISTORY,
            extras = [])
    @JvmStatic
    fun clickMenuHistory() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.HISTORY).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - MyShots",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.CAPTURE,
            extras = [])
    @JvmStatic
    fun clickMenuCapture() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CAPTURE).queue()
    }

    @TelemetryDoc(
            name = "Click Panel - Bookmarks",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun showPanelBookmark() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Click Panel - Downloads",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.DOWNLOAD,
            extras = [])
    @JvmStatic
    fun showPanelDownload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.DOWNLOAD).queue()
    }

    @TelemetryDoc(
            name = "Click Panel - History",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.HISTORY,
            extras = [])
    @JvmStatic
    fun showPanelHistory() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.HISTORY).queue()
    }

    @TelemetryDoc(
            name = "Click Panel - MyShots",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.CAPTURE,
            extras = [])
    @JvmStatic
    fun showPanelCapture() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.CAPTURE).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - TurboMode",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.MENU,
            value = Value.TURBO,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
    @JvmStatic
    fun menuTurboChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.TURBO)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Block Images",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.MENU,
            value = Value.BLOCK_IMAGE,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
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

    @TelemetryDoc(
            name = "Click Menu - Clear cache",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.CLEAR_CACHE,
            extras = [])
    @JvmStatic
    fun clickMenuClearCache() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.CLEAR_CACHE).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Settings",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.SETTINGS,
            extras = [])
    @JvmStatic
    fun clickMenuSettings() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, SETTINGS).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Exit",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.EXIT,
            extras = [])
    @JvmStatic
    fun clickMenuExit() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.EXIT).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Bookmarks",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun clickMenuBookmark() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Forward",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.FORWARD,
            extras = [])
    @JvmStatic
    fun clickToolbarForward() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.FORWARD).queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Reload",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.RELOAD,
            extras = [])
    @JvmStatic
    fun clickToolbarReload() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD).queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Share Link",
            action = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.TOOLBAR,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun clickToolbarShare() {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Add bookmark",
            action = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.TOOLBAR,
            value = Value.BOOKMARK,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
    @JvmStatic
    fun clickToolbarBookmark(isAdd: Boolean) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.BOOKMARK)
                .extra(Extra.TO, java.lang.Boolean.toString(isAdd))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Pin shortcut",
            action = Category.ACTION,
            method = Method.PIN_SHORTCUT,
            `object` = Object.TOOLBAR,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun clickAddToHome() {
        EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.TOOLBAR, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Take Screenshot",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.CAPTURE,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "ping version"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category name"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun clickToolbarCapture(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.CAPTURE)
                .extra(Extra.VERSION, Integer.toString(TOOL_BAR_CAPTURE_TELEMETRY_VERSION))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Top Site",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [TelemetryExtra(name = Extra.ON, value = "Top Site Position")])
    @JvmStatic
    fun clickTopSiteOn(index: Int) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, Integer.toString(index))
                .queue()

        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOPSITE)
                .queue()
    }

    @TelemetryDoc(
            name = "Remove Top Site",
            action = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [TelemetryExtra(name = Extra.DEFAULT, value = "true,false")])
    @JvmStatic
    fun removeTopSite(isDefault: Boolean) {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.HOME, Value.LINK)
                .extra(Extra.DEFAULT, java.lang.Boolean.toString(isDefault))
                //  TODO: add index
                .queue()
    }

    @TelemetryDoc(
            name = "Search in Home and add a tab",
            action = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.HOME,
            extras = [])
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

    @TelemetryDoc(
            name = "Enter an url in SearchBar",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.SEARCH_BAR,
            value = Value.LINK,
            extras = [])
    private fun browseEvent() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Use SearchSuggestion SearchBar",
            action = Category.ACTION,
            method = Method.TYPE_SELECT_QUERY,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [])
    @JvmStatic
    fun searchSelectEvent() {
        val telemetry = TelemetryHolder.get()

        EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_SUGGESTION, searchEngine.identifier)
    }

    @TelemetryDoc(
            name = "Search with text in SearchBar",
            action = Category.ACTION,
            method = Method.TYPE_QUERY,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [])
    private fun searchEnterEvent() {
        val telemetry = TelemetryHolder.get()

        EventBuilder(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR).queue()

        val searchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(
                telemetry.configuration.context)

        telemetry.recordSearch(SearchesMeasurement.LOCATION_ACTIONBAR, searchEngine.identifier)
    }

    @TelemetryDoc(
            name = "Toggle Private Mode",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.PRIVATE_MODE,
            value = Value.ENTER + "," + Value.EXIT,
            extras = [])
    @JvmStatic
    fun togglePrivateMode(enter: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.PRIVATE_MODE, if (enter) Value.ENTER else Value.EXIT).queue()
    }

    @TelemetryDoc(
            name = "Long click on Search Suggestion",
            action = Category.ACTION,
            method = Method.LONG_PRESS,
            `object` = Object.SEARCH_SUGGESTION,
            value = "",
            extras = [])
    @JvmStatic
    fun searchSuggestionLongClick() {
        EventBuilder(Category.ACTION, Method.LONG_PRESS, Object.SEARCH_SUGGESTION).queue()
    }

    @TelemetryDoc(
            name = "Clear SearchBar",
            action = Category.ACTION,
            method = Method.CLEAR,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [TelemetryExtra(name = Extra.VERSION, value = SEARCHCLEAR_TELEMETRY_VERSION)])
    @JvmStatic
    fun searchClear() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.SEARCH_BAR)
                .extra(Extra.VERSION, SEARCHCLEAR_TELEMETRY_VERSION)
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss SearchBar",
            action = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [TelemetryExtra(name = Extra.VERSION, value = SEARCHDISMISS_TELEMETRY_VERSION)])
    @JvmStatic
    fun searchDismiss() {
        EventBuilder(Category.ACTION, Method.CANCEL, Object.SEARCH_BAR)
                .extra(Extra.VERSION, SEARCHDISMISS_TELEMETRY_VERSION)
                .queue()
    }

    @TelemetryDoc(
            name = "Show SearchBar from Home",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SEARCH_BAR,
            value = Value.SEARCH_BOX,
            extras = [])
    @JvmStatic
    fun showSearchBarHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BOX).queue()
    }

    @TelemetryDoc(
            name = "Show SearchBar by clicking MINI_URLBAR",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SEARCH_BAR,
            value = Value.MINI_URLBAR,
            extras = [])
    @JvmStatic
    fun clickUrlbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.MINI_URLBAR).queue()
    }

    @TelemetryDoc(
            name = "Show SearchBar by clicking SEARCH_BUTTON",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SEARCH_BAR,
            value = Value.SEARCH_BUTTON,
            extras = [])
    @JvmStatic
    fun clickToolbarSearch() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON).queue()
    }

    @TelemetryDoc(
            name = "Add Tab from Toolbar",
            action = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.TOOLBAR,
            extras = [])
    @JvmStatic
    fun clickAddTabToolbar() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOOLBAR).queue()
    }

    @TelemetryDoc(
            name = "Add Tab from TabTray",
            action = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun clickAddTabTray() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Switch Tab From TabTray",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun clickTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.TAB, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Remove Tab From TabTray",
            action = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun closeTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.TAB, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Swipe Tab From TabTray",
            action = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun swipeTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.TAB, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Close all From TabTray",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CLOSE_ALL,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun closeAllTabFromTabTray() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CLOSE_ALL, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Remove Download File",
            action = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.PANEL,
            value = Value.FILE,
            extras = [])
    @JvmStatic
    fun downloadRemoveFile() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.FILE).queue()
    }


    @TelemetryDoc(
            name = "Delete Download File",
            action = Category.ACTION,
            method = Method.DELETE,
            `object` = Object.PANEL,
            value = Value.FILE,
            extras = [])
    @JvmStatic
    fun downloadDeleteFile() {
        EventBuilder(Category.ACTION, Method.DELETE, Object.PANEL, Value.FILE).queue()
    }

    @TelemetryDoc(
            name = "Open Download File via snackbar",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.FILE,
            extras = [(TelemetryExtra(name = Extra.SNACKBAR, value = "true,false"))])
    @JvmStatic
    fun downloadOpenFile(fromSnackBar: Boolean) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.FILE)
                .extra(Extra.SNACKBAR, java.lang.Boolean.toString(fromSnackBar))
                .queue()
    }

    @TelemetryDoc(
            name = "Show File ContextMenu",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.DOWNLOAD,
            extras = [])
    @JvmStatic
    fun showFileContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.DOWNLOAD).queue()
    }

    @TelemetryDoc(
            name = "History Open Link",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun historyOpenLink() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "History Remove Link",
            action = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.PANEL,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun historyRemoveLink() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Bookmark Remove Item",
            action = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.PANEL,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun bookmarkRemoveItem() {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.PANEL, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Bookmark Edit Item",
            action = Category.ACTION,
            method = Method.EDIT,
            `object` = Object.PANEL,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun bookmarkEditItem() {
        EventBuilder(Category.ACTION, Method.EDIT, Object.PANEL, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Bookmark Open Item",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun bookmarkOpenItem() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Show History ContextMenu",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.HISTORY,
            extras = [])
    @JvmStatic
    fun showHistoryContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.HISTORY).queue()
    }

    @TelemetryDoc(
            name = "Show Bookmark ContextMenu",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.BOOKMARK,
            extras = [])
    @JvmStatic
    fun showBookmarkContextMenu() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.BOOKMARK).queue()
    }

    @TelemetryDoc(
            name = "Clear History",
            action = Category.ACTION,
            method = Method.CLEAR,
            `object` = Object.PANEL,
            value = Value.HISTORY,
            extras = [])
    @JvmStatic
    fun clearHistory() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.PANEL, Value.HISTORY).queue()
    }

    @TelemetryDoc(
            name = "Open Capture Item",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.CAPTURE,
            extras = [])
    @JvmStatic
    fun openCapture() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.CAPTURE).queue()
    }

    @TelemetryDoc(
            name = "Open Capture Link",
            action = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.CAPTURE,
            value = Value.LINK,
            extras = [TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun openCaptureLink(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.CAPTURE, Value.LINK)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Edit Capture Image",
            action = Category.ACTION,
            method = Method.EDIT,
            `object` = Object.CAPTURE,
            value = Value.IMAGE,
            extras = [TelemetryExtra(name = Extra.SUCCESS, value = "true,false"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun editCaptureImage(editAppResolved: Boolean, category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.EDIT, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SUCCESS, java.lang.Boolean.toString(editAppResolved))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Share Capture Image",
            action = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.CAPTURE,
            value = Value.IMAGE,
            extras = [TelemetryExtra(name = Extra.SNACKBAR, value = "true,false"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun shareCaptureImage(fromSnackBar: Boolean, category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.SNACKBAR, java.lang.Boolean.toString(fromSnackBar))
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Show Capture Info",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.CAPTURE,
            value = Value.INFO,
            extras = [TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun showCaptureInfo(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.CAPTURE, Value.INFO)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Delete Capture Image",
            action = Category.ACTION,
            method = Method.DELETE,
            `object` = Object.CAPTURE,
            value = Value.IMAGE,
            extras = [TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version")
            ])
    @JvmStatic
    fun deleteCaptureImage(category: String, categoryVersion: Int) {
        EventBuilder(Category.ACTION, Method.DELETE, Object.CAPTURE, Value.IMAGE)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .queue()
    }

    @TelemetryDoc(
            name = "Delete Capture Image",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FEEDBACK,
            value = "dismiss,positive,negative",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "contextual_hints,settings")
            ])
    @JvmStatic
    fun feedbackClickEvent(value: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Feedback Dialog",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FEEDBACK,
            value = "",
            extras = [])
    @JvmStatic
    fun showFeedbackDialog() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK).queue()
    }

    @TelemetryDoc(
            name = "Show Rate AppNotification",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FEEDBACK,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "notification")])
    @JvmStatic
    fun showRateAppNotification() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Rate AppNotification",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FEEDBACK,
            value = "null,positive,negative",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "notification")])
    // TODO: test Context from contetReceiver
    @JvmStatic
    fun clickRateAppNotification(value: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    // document is the same as above
    @JvmStatic
    fun clickRateAppNotification() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, RATE_APP_NOTIFICATION_TELEMETRY_VERSION.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Default Browser Notification shown",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.DEFAULT_BROWSER,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "notification")])
    @JvmStatic
    fun showDefaultSettingNotification() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @TelemetryDoc(
            name = "Default Browser Notification Clicked",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DEFAULT_BROWSER,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = TelemetryWrapper.Extra_Value.NOTIFICATION),
                TelemetryExtra(name = Extra.VERSION, value = "version")])
    @JvmStatic
    fun clickDefaultSettingNotification() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DEFAULT_BROWSER)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .extra(Extra.VERSION, Integer.toString(DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION))
                .queue()
    }

    @TelemetryDoc(
            name = "Default Browser Service Failed",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.DEFAULT_BROWSER,
            value = "",
            extras = [TelemetryExtra(name = Extra.SUCCESS, value = "true,false")])
    @JvmStatic
    fun onDefaultBrowserServiceFailed() {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.DEFAULT_BROWSER)
                .extra(Extra.SUCCESS, java.lang.Boolean.toString(false))
                .queue()
    }


    @TelemetryDoc(
            name = "Promote Share Dialog Clicked",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PROMOTE_SHARE,
            value = "dismiss,share",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "contextual_hints,settings")])
    @JvmStatic
    fun promoteShareClickEvent(value: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PROMOTE_SHARE, value)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Promote Share Dialog shown",
            action = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.PROMOTE_SHARE,
            value = "",
            extras = [])
    @JvmStatic
    fun showPromoteShareDialog() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.PROMOTE_SHARE).queue()
    }

    @TelemetryDoc(
            name = "Change Theme To",
            action = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.THEMETOY,
            value = "",
            extras = [TelemetryExtra(name = Extra.TO, value = "theme name")])
    @JvmStatic
    fun changeThemeTo(themeName: String) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.THEMETOY)
                .extra(Extra.TO, themeName)
                .queue()
    }

    @TelemetryDoc(
            name = "Reset Theme To",
            action = Category.ACTION,
            method = Method.RESET,
            `object` = Object.THEMETOY,
            value = "",
            extras = [TelemetryExtra(name = Extra.TO, value = "default")])
    @JvmStatic
    fun resetThemeToDefault() {
        EventBuilder(Category.ACTION, Method.RESET, Object.THEMETOY)
                .extra(Extra.TO, Extra_Value.DEFAULT)
                .queue()
    }

    @TelemetryDoc(
            name = "Erase Private Mode Notification",
            action = Category.ACTION,
            method = Method.CLEAR,
            `object` = Object.PRIVATE_MODE,
            value = "",
            extras = [TelemetryExtra(name = Extra.TO, value = "notification")])
    @JvmStatic
    fun erasePrivateModeNotification() {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.PRIVATE_MODE)
                .extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Banner Background",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.BANNER,
            value = Value.BACKGROUND,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "page Id")])
    @JvmStatic
    fun clickBannerBackground(pageId: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.BANNER, Value.BACKGROUND)
                .extra(Extra.SOURCE, pageId)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Banner Item",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.BANNER,
            value = Value.ITEM,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "pageId"),
                TelemetryExtra(name = Extra.ON, value = "position")])
    @JvmStatic
    fun clickBannerItem(pageId: String, itemPosition: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.BANNER, Value.ITEM)
                .extra(Extra.SOURCE, pageId)
                .extra(Extra.ON, Integer.toString(itemPosition))
                .queue()
    }

    @TelemetryDoc(
            name = "Swipe Banner Item",
            action = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.BANNER,
            value = Value.PAGE,
            extras = [TelemetryExtra(name = Extra.DIRECTION, value = "direction"),
                TelemetryExtra(name = Extra.TO, value = "position")])
    @JvmStatic
    fun swipeBannerItem(directionX: Int, toItemPosition: Int) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.BANNER, Value.PAGE)
                .extra(Extra.DIRECTION, Integer.toString(directionX))
                .extra(Extra.TO, Integer.toString(toItemPosition))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Wifi Finder Survey",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.HOME,
            value = Value.WIFI_FINDER,
            extras = [])
    @JvmStatic
    fun clickWifiFinderSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.HOME, Value.WIFI_FINDER)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Wifi Finder Survey Feedback",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DOORHANGER,
            value = "negative,positive",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = Value.WIFI_FINDER)])
    @JvmStatic
    fun clickWifiFinderSurveyFeedback(positive: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, if (positive) Value.POSITIVE else Value.NEGATIVE)
                .extra(Extra.SOURCE, Value.WIFI_FINDER)
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss Wifi Finder Survey Feedback",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DOORHANGER,
            value = Value.DISMISS,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = Value.WIFI_FINDER)])
    @JvmStatic
    fun dismissWifiFinderSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, Value.DISMISS)
                .extra(Extra.SOURCE, Value.WIFI_FINDER)
                .queue()
    }

    @TelemetryDoc(
            name = "Click VPN Survey",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.HOME,
            value = Value.VPN,
            extras = [])
    @JvmStatic
    fun clickVpnSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.HOME, Value.VPN)
                .queue()
    }

    @TelemetryDoc(
            name = "Click VPN Survey Feedback",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DOORHANGER,
            value = "negative,positive",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = Value.VPN)])
    @JvmStatic
    fun clickVpnSurveyFeedback(positive: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, if (positive) Value.POSITIVE else Value.NEGATIVE)
                .extra(Extra.SOURCE, Value.VPN)
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss VPN Survey",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DOORHANGER,
            value = Value.DISMISS,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = Value.VPN)])
    @JvmStatic
    fun dismissVpnSurvey() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, Value.DISMISS)
                .extra(Extra.SOURCE, Value.VPN)
                .queue()
    }


    @JvmStatic
    fun findInPage(type: FIND_IN_PAGE) {
        val builder = when (type) {

            OPEN_BY_MENU -> clickMenuFindInPage()
            CLICK_PREVIOUS -> clickFindInPagePrevious()
            CLICK_NEXT -> clickFindInPageNext()
            DISMISS_BY_BACK -> cancelFindInPageBack()
            DISMISS_BY_CLOSE -> cancelFindInPageClose()
            DISMISS -> cancelFindInPageOther()
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

    @TelemetryDoc(
            name = "Cancel FindInPage - others",
            action = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.FIND_IN_PAGE,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "other"), TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun cancelFindInPageOther() =
            EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
                    .extra(Extra.SOURCE, "other")
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Cancel FindInPage using close button",
            action = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.FIND_IN_PAGE,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "close_button"), TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun cancelFindInPageClose() =
            EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
                    .extra(Extra.SOURCE, "close_button")
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Cancel FindInPage using back button",
            action = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.FIND_IN_PAGE,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "back_button"), TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun cancelFindInPageBack() = EventBuilder(Category.ACTION, Method.CANCEL, Object.FIND_IN_PAGE, null)
            .extra(Extra.SOURCE, "back_button")
            .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Click FindInPage Previous",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FIND_IN_PAGE,
            value = Value.NEXT,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickFindInPageNext() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.NEXT)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Click FindInPage Previous",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FIND_IN_PAGE,
            value = Value.PREVIOUS,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickFindInPagePrevious() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.PREVIOUS)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Click Menu FindInPage",
            action = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.FIND_IN_PAGE,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickMenuFindInPage() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.FIND_IN_PAGE)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))


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
