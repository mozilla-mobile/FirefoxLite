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
import android.os.StrictMode.ThreadPolicy.Builder
import android.preference.PreferenceManager
import android.util.Log
import android.webkit.PermissionRequest
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.provider.ScreenshotContract
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_NEXT
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_PREVIOUS
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.OPEN_BY_MENU
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.SETTINGS
import org.mozilla.focus.utils.AdjustHelper
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.strictmodeviolator.StrictModeViolation
import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder
import org.mozilla.telemetry.annotation.TelemetryDoc
import org.mozilla.telemetry.annotation.TelemetryExtra
import org.mozilla.telemetry.config.TelemetryConfiguration
import org.mozilla.telemetry.event.TelemetryEvent
import org.mozilla.telemetry.measurement.DefaultSearchMeasurement
import org.mozilla.telemetry.measurement.EventsMeasurement
import org.mozilla.telemetry.measurement.SearchesMeasurement
import org.mozilla.telemetry.measurement.SettingsMeasurement
import org.mozilla.telemetry.measurement.TelemetryMeasurement
import org.mozilla.telemetry.net.TelemetryClient
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder
import org.mozilla.telemetry.ping.TelemetryEventPingBuilder
import org.mozilla.telemetry.ping.TelemetryPingBuilder
import org.mozilla.telemetry.schedule.jobscheduler.JobSchedulerTelemetryScheduler
import org.mozilla.telemetry.serialize.JSONPingSerializer
import org.mozilla.telemetry.storage.FileTelemetryStorage
import org.mozilla.threadutils.ThreadUtils
import java.util.concurrent.atomic.AtomicInteger

object TelemetryWrapper {
    private const val TELEMETRY_APP_NAME_ZERDA = "Zerda"

    private const val TOOL_BAR_CAPTURE_TELEMETRY_VERSION = 3
    private const val RATE_APP_NOTIFICATION_TELEMETRY_VERSION = 3
    private const val DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION = 2
    private const val OPEN_HOME_LINK_VERSION = "2"
    private const val FIND_IN_PAGE_VERSION = 2
    private const val SEARCHCLEAR_TELEMETRY_VERSION = "2"
    private const val SEARCHDISMISS_TELEMETRY_VERSION = "2"
    private const val LIFE_FEED_PROMO_VERSION = "2"

    // List of site title which are the partners of affiliate program.
    // Please use the exact title name from pin_sites.json
    private val AFFILIATE_SITES = listOf("Bukalapak", "Tokopedia")

    @JvmStatic
    private val sRefCount = AtomicInteger(0)

    internal object Category {
        const val ACTION = "action"
        const val EVENT_DOWNLOADS = "Downloads"
        const val SEARCH = "search"
        const val ENTER_LANDSCAPE_MODE = "enter landscape mode"
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
        const val GET = "get"

        const val FOREGROUND = "foreground"
        const val BACKGROUND = "background"
        const val SHARE = "share"
        const val PIN_SHORTCUT = "pin_shortcut"
        const val SAVE = "save"
        const val COPY = "copy"
        const val OPEN = "open"
        const val SHOW = "show"
        const val LAUNCH = "launch"
        const val KILL = "kill"
    }

    internal object Object {
        const val PRIVATE_MODE = "private_mode"
        const val PRIVATE_SHORTCUT = "private_shortcut"
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
        const val FIRSTRUN_PUSH = "firstrun_push"

        const val FEEDBACK = "feedback"
        const val DEFAULT_BROWSER = "default_browser"
        const val PROMOTE_SHARE = "promote_share"
        const val THEMETOY = "themetoy"
        const val BANNER = "banner"
        const val DOORHANGER = "doorhanger"
        const val VPN_DOORHANGER = "vpn_doorhanger"
        const val QUICK_SEARCH = "quicksearch"

        const val LANDSCAPE_MODE = "landscape_mode"
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
        internal const val LIFEFEED_NEWS = "lifefeed_news"
        internal const val LIFEFEED_EC = "lifefeed_ec"
        internal const val LIFEFEED_PROMO = "lifefeed_promo"

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
        internal const val PRIVATE_MODE = "private_mode"

        internal const val BACKGROUND = "background"
        internal const val ITEM = "item"
        internal const val PAGE = "page"
        internal const val WIFI_FINDER = "wifi_finder"
        internal const val VPN = "vpn"
        internal const val VPN_RECOMMEND = "vpn_recommend"

        internal const val PREVIOUS = "previous"
        internal const val NEXT = "next"

        internal const val NIGHT_MODE = "night_mode"
        internal const val NIGHT_MODE_BRIGHTNESS = "night_mode_brightness"

        internal const val NEW = "new"
        internal const val UPDATE = "update"
        internal const val RETURN = "return"
        internal const val SWIPE = "swipe"

        internal const val SETTINGS_PRIVATE_SHORTCUT = "pref_private_shortcut"
    }

    internal object Extra {
        const val TO = "to"
        const val FROM = "from"
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
        const val VPN_INSTALLED = "vpn_installed"
        const val ENGINE = "engine"
        const val DELAY = "delay"
        const val MESSAGE = "message"
        const val POSITION = "position"
        const val FEED = "feed"
        const val SUB_CATEGORY = "subcategory"
        const val MODE = "mode"
        const val ID = "id"
        const val DURATION = "duration"
    }

    object Extra_Value {
        const val SETTING = "settings"
        const val CONTEXTUAL_HINTS = "contextual_hints"
        const val NOTIFICATION = "notification"
        internal const val WEB_SEARCH = "web_search"
        internal const val TEXT_SELECTION = "text_selection"
        internal const val DEFAULT = "default"
        const val TAB = "tab"
        const val ARROW = "arrow"
        internal const val WEBVIEW = "webview"
        internal const val MENU = "menu"
        internal const val PRIVATE_MODE = "private_mode"
        internal const val SYSTEM_BACK = "system_back"
        const val LAUNCHER = "launcher"
        const val EXTERNAL_APP = "external_app"
    }

    enum class FIND_IN_PAGE {
        OPEN_BY_MENU,
        CLICK_PREVIOUS,
        CLICK_NEXT
    }

    // context passed here is nullable cause it may come from Java code
    @JvmStatic
    fun isTelemetryEnabled(context: Context?): Boolean {
        return if (context == null) {
            false
        } else {
            Inject.isTelemetryEnabled(context)
        }
    }

    @JvmStatic
    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val key = resources.getString(R.string.pref_key_telemetry)
        preferences.edit()
                .putBoolean(key, enabled)
                .apply()

        ThreadUtils.postToBackgroundThread {
            // We want to queue this ping and send asap.
            TelemetryWrapper.settingsEvent(key, enabled.toString(), true)

            // If there are things already collected, we'll still upload them.
            TelemetryHolder.get()
                    .configuration
                    .isCollectionEnabled = enabled
        }
    }

    fun init(context: Context) {
        StrictModeViolation.tempGrant({ obj: Builder -> obj.permitDiskReads().permitDiskWrites() }) {
            // When initializing the telemetry library it will make sure that all directories exist and
            // are readable/writable.
            val resources = context.resources

            val telemetryEnabled = isTelemetryEnabled(context)

            updateDefaultBrowserStatus(context)

            val trackerTokenPrefKey = resources.getString(R.string.pref_key_s_tracker_token)
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
                            resources.getString(R.string.pref_s_news),
                            resources.getString(R.string.pref_key_locale),
                            trackerTokenPrefKey
                    )
                    .setSettingsProvider(CustomSettingsProvider())
                    .setCollectionEnabled(telemetryEnabled)
                    .setUploadEnabled(true) // the default value for UploadEnabled is true, but we want to make it clear.

            FirebaseHelper.init(context, telemetryEnabled)

            updateFirebaseUserPropertiesAsync(configuration)

            val serializer = JSONPingSerializer()
            val storage = FileTelemetryStorage(configuration, serializer)
            val client = TelemetryClient(HttpURLConnectionClient())
            val scheduler = JobSchedulerTelemetryScheduler()

            TelemetryHolder.set(
                    Telemetry(configuration, storage, client, scheduler)
                            .addPingBuilder(TelemetryCorePingBuilder(configuration))
                            .addPingBuilder(TelemetryEventPingBuilder(configuration))
                            .setDefaultSearchProvider(createDefaultSearchProvider(context))
            )
        }
    }

    private fun updateFirebaseUserPropertiesAsync(configuration: TelemetryConfiguration) {
        ThreadUtils.postToBackgroundThread {
            val provider = CustomSettingsProvider().apply { update(configuration) }
            val context = configuration.context

            configuration.preferencesImportantForTelemetry?.forEach { key ->
                val value = if (provider.containsKey(key)) {
                    provider.getValue(key).toString()
                } else {
                    ""
                }

                val propertyKey = convertToPropertyKey(context, key)
                FirebaseHelper.setUserProperty(configuration.context, propertyKey, value)
            }
        }
    }

    private fun convertToPropertyKey(context: Context, key: String): String {
        return if (key == context.getString(R.string.pref_key_s_tracker_token)) {
            FirebaseHelper.USER_PROPERTY_TRACKER
        } else {
            key
        }
    }

    private fun updateDefaultBrowserStatus(context: Context) {
        Settings.updatePrefDefaultBrowserIfNeeded(context, Browsers.isDefaultBrowser(context))
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FIRSTRUN,
            value = Value.FINISH,
            extras = [TelemetryExtra(name = Extra.ON, value = "time spent on First Run")])
    @JvmStatic
    fun finishFirstRunEvent(duration: Long, mode: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN, Value.FINISH)
                .extra(Extra.ON, java.lang.Long.toString(duration))
                .extra(Extra.MODE, Integer.toString(mode))
                .queue()
    }

    @TelemetryDoc(
            name = "App is launched by Launcher",
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.SHORTCUT,
            extras = [])
    @JvmStatic
    fun launchByHomeScreenShortcutEvent() {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.SHORTCUT).queue()
    }

    @TelemetryDoc(
            name = "App is launched from Private Shortcut",
            category = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.PRIVATE_MODE,
            extras = [TelemetryExtra(name = Extra.FROM, value = "[${Extra_Value.LAUNCHER}|${Extra_Value.EXTERNAL_APP}]")])
    @JvmStatic
    fun launchByPrivateModeShortcut(from: String) {
        EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.PRIVATE_MODE)
                .extra(Extra.FROM, from)
                .queue()
    }

    @TelemetryDoc(
            name = "Show private shortcut prompt",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.PRIVATE_SHORTCUT,
            value = "",
            extras = [TelemetryExtra(name = Extra.MODE, value = Extra_Value.PRIVATE_MODE)])
    @JvmStatic
    fun showPrivateShortcutPrompt() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.PRIVATE_SHORTCUT)
                .extra(Extra.MODE, Extra_Value.PRIVATE_MODE)
                .queue()
    }

    @TelemetryDoc(
            name = "Click private shortcut prompt",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PRIVATE_SHORTCUT,
            value = "[${Value.POSITIVE}|${Value.NEGATIVE}|${Value.DISMISS}]",
            extras = [TelemetryExtra(name = Extra.MODE, value = Extra_Value.PRIVATE_MODE)])
    @JvmStatic
    fun clickPrivateShortcutPrompt(value: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PRIVATE_SHORTCUT, value)
                .extra(Extra.MODE, Extra_Value.PRIVATE_MODE)
                .queue()
    }

    @TelemetryDoc(
            name = "Users clicked on a Setting",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SETTING,
            value = Value.SETTINGS_PRIVATE_SHORTCUT,
            extras = [])
    @JvmStatic
    fun clickPrivateShortcutItemInSettings() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, Value.SETTINGS_PRIVATE_SHORTCUT)
                .queue()
    }

    @TelemetryDoc(
            name = "Exit private mode",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PRIVATE_MODE,
            value = Value.EXIT,
            extras = [
                TelemetryExtra(name = Extra.FROM, value = "[${Extra_Value.SYSTEM_BACK}]"),
                TelemetryExtra(name = Extra.MODE, value = Extra_Value.PRIVATE_MODE)
            ])
    @JvmStatic
    fun exitPrivateMode(from: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PRIVATE_MODE, Value.EXIT)
                .extra(Extra.FROM, from)
                .extra(Extra.MODE, Extra_Value.PRIVATE_MODE)
                .queue()
    }

    @TelemetryDoc(
            name = "Private shortcut created",
            category = Category.ACTION,
            method = Method.PIN_SHORTCUT,
            `object` = Object.PRIVATE_SHORTCUT,
            value = "",
            extras = [TelemetryExtra(name = Extra.MODE, value = Extra_Value.PRIVATE_MODE)])
    @JvmStatic
    fun createPrivateShortcut() {
        EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.PRIVATE_SHORTCUT)
                .extra(Extra.MODE, Extra_Value.PRIVATE_MODE)
                .queue()
    }

    @TelemetryDoc(
            name = "Kill app",
            category = Category.ACTION,
            method = Method.KILL,
            `object` = Object.APP,
            value = "",
            extras = [TelemetryExtra(name = Extra.MODE, value = Extra_Value.PRIVATE_MODE)])
    @JvmStatic
    fun appKilled(mode: String) {
        EventBuilder(Category.ACTION, Method.KILL, Object.APP)
                .extra(Extra.MODE, mode)
                .queue()
    }

    @TelemetryDoc(
            name = "App is launched by external app",
            category = Category.ACTION,
            method = Method.LAUNCH,
            `object` = Object.APP,
            value = Value.EXTERNAL_APP,
            extras = [TelemetryExtra(name = Extra.TYPE, value = Extra_Value.TEXT_SELECTION + "," + Extra_Value.WEB_SEARCH)])
    @JvmStatic
    fun launchByExternalAppEvent(value: String?) {
        if (value == null) {
            EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP).queue()
        } else {
            EventBuilder(Category.ACTION, Method.LAUNCH, Object.APP, Value.EXTERNAL_APP)
                    .extra(Extra.TYPE, value)
                    .queue()
        }
    }

    @TelemetryDoc(
            name = "Users changed a Setting",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.SETTING,
            value = "settings pref key",
            extras = [TelemetryExtra(name = Extra.TO, value = "New Value for the pref")])
    @JvmStatic
    fun settingsEvent(key: String, value: String, sendNow: Boolean = false) {
        // We only log whitelist-ed setting
        val validPrefKey = FirebaseEvent.getValidPrefKey(key)
        if (validPrefKey != null) {
            EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, validPrefKey)
                    .extra(Extra.TO, value)
                    .queue(sendNow)
        }
    }

    @TelemetryDoc(
            name = "Users clicked on a Setting",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "Users change Locale in Settings",
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.FOREGROUND,
            `object` = Object.APP,
            value = "",
            extras = [])
    @JvmStatic
    fun startSession() {
        if (sRefCount.getAndIncrement() == 0) {
            TelemetryHolder.get().recordSessionStart()
        }

        EventBuilder(Category.ACTION, Method.FOREGROUND, Object.APP).queue()
    }

    @TelemetryDoc(
            name = "Session ends",
            category = Category.ACTION,
            method = Method.BACKGROUND,
            `object` = Object.APP,
            value = "",
            extras = [])
    @JvmStatic
    fun stopSession() {
        if (sRefCount.decrementAndGet() == 0) {
            TelemetryHolder.get().recordSessionEnd()
        }

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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.TABTRAY,
            value = Value.TOOLBAR,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun showTabTrayToolbar(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.TOOLBAR)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Show Menu from Toolbar",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MENU,
            value = Value.TOOLBAR,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun showMenuToolbar(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MENU, Value.TOOLBAR)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Downloads",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "Click Menu - Night Mode",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.MENU,
            value = Value.NIGHT_MODE,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
    @JvmStatic
    fun menuNightModeChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.NIGHT_MODE)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Block Images",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.MENU,
            value = Value.BLOCK_IMAGE,
            extras = [TelemetryExtra(name = Extra.TO, value = "true,false")])
    @JvmStatic
    fun menuBlockImageChangeTo(enable: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.MENU, Value.BLOCK_IMAGE)
                .extra(Extra.TO, java.lang.Boolean.toString(enable))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Clear cache",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.FORWARD,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickToolbarForward(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.FORWARD)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Reload",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.RELOAD,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickToolbarReload(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Share Link",
            category = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.TOOLBAR,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickToolbarShare(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Add bookmark",
            category = Category.ACTION,
            method = Method.SHARE,
            `object` = Object.TOOLBAR,
            value = Value.BOOKMARK,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.TO, value = "true,false"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickToolbarBookmark(isAdd: Boolean, mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.BOOKMARK)
                .extra(Extra.VERSION, "2")
                .extra(Extra.TO, java.lang.Boolean.toString(isAdd))
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
        if (isAdd) {
            AdjustHelper.trackEvent(EVENT_SAVE_BOOKMKARK)
        }
    }

    @TelemetryDoc(
            name = "Click Toolbar - Pin shortcut",
            category = Category.ACTION,
            method = Method.PIN_SHORTCUT,
            `object` = Object.TOOLBAR,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickAddToHome(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.TOOLBAR, Value.LINK)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
        AdjustHelper.trackEvent(EVENT_ADD_TO_HOMESCREEN)
    }

    @TelemetryDoc(
            name = "Click Toolbar - Take Screenshot",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.CAPTURE,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "4"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category name"),
                TelemetryExtra(name = Extra.CATEGORY_VERSION, value = "category version"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "0-4")
            ])
    @JvmStatic
    fun clickToolbarCapture(category: String, categoryVersion: Int, mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.CAPTURE)
                .extra(Extra.VERSION, "4")
                .extra(Extra.CATEGORY, category)
                .extra(Extra.CATEGORY_VERSION, Integer.toString(categoryVersion))
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
        AdjustHelper.trackEvent(EVENT_TAKE_SCREENSHOT)
    }

    @TelemetryDoc(
            name = "Click Top Site",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [TelemetryExtra(name = Extra.ON, value = "Top Site Position"),
                TelemetryExtra(name = Extra.SOURCE, value = "Preset Top Site like **"),
                TelemetryExtra(name = Extra.VERSION, value = OPEN_HOME_LINK_VERSION)
            ])
    @JvmStatic
    fun clickTopSiteOn(index: Int, source: String) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, Integer.toString(index))
                .extra(Extra.SOURCE, source)
                .extra(Extra.VERSION, OPEN_HOME_LINK_VERSION)
                .queue()

        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOPSITE)
                .queue()

        // Record a separate click count from those affiliate program partners.
        if (AFFILIATE_SITES.contains(source)) {
            AdjustHelper.trackEvent(EVENT_CLICK_AFFILIATE_LINK)
        }
    }

    @TelemetryDoc(
            name = "Remove Top Site",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.SEARCH_BAR,
            value = Value.LINK,
            extras = [])
    private fun browseEvent() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK).queue()
    }

    @TelemetryDoc(
            name = "Use SearchSuggestion SearchBar",
            category = Category.ACTION,
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

        AdjustHelper.trackEvent(EVENT_START_SEARCH)
    }

    @TelemetryDoc(
            name = "Search with text in SearchBar",
            category = Category.ACTION,
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

        AdjustHelper.trackEvent(EVENT_START_SEARCH)
    }

    @TelemetryDoc(
            name = "Toggle Private Mode",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.PRIVATE_MODE,
            value = Value.ENTER + "," + Value.EXIT,
            extras = [])
    @JvmStatic
    fun togglePrivateMode(enter: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.PRIVATE_MODE, if (enter) Value.ENTER else Value.EXIT).queue()
        if (enter) {
            AdjustHelper.trackEvent(EVENT_ENTER_PRIVATE_MODE)
        }
    }

    @TelemetryDoc(
            name = "Long click on Search Suggestion",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SEARCH_BAR,
            value = Value.SEARCH_BUTTON,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickToolbarSearch(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Add Tab from Toolbar",
            category = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.TOOLBAR,
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = "2"),
                TelemetryExtra(name = Extra.MODE, value = "[webview|menu]"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]")
            ])
    @JvmStatic
    fun clickAddTabToolbar(mode: String, position: Int) {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOOLBAR)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .queue()
    }

    @TelemetryDoc(
            name = "Add Tab from TabTray",
            category = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun clickAddTabTray() {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Enter Private Mode from TabTray",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PRIVATE_MODE,
            value = Value.TABTRAY,
            extras = [])
    @JvmStatic
    fun privateModeTray() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PRIVATE_MODE, Value.TABTRAY).queue()
    }

    @TelemetryDoc(
            name = "Switch Tab From TabTray",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "Open lifefeed news",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_NEWS,
            extras = [])
    @JvmStatic
    fun openLifeFeedNews() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LIFEFEED_NEWS)
                .queue()
    }

    @TelemetryDoc(
            name = "Click on news item",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_NEWS,
            extras = [TelemetryExtra(name = Extra.POSITION, value = "1,2,3..."),
                    TelemetryExtra(name = Extra.FEED, value = "Newspoint,DainikBhaskar.com"),
                    TelemetryExtra(name = Extra.SOURCE, value = "India TV,Business World,HW News English...etc"),
                    TelemetryExtra(name = Extra.CATEGORY, value = "Uttar Pradesh,National,Tech Knowledge....etc"),
                    TelemetryExtra(name = Extra.SUB_CATEGORY, value = "top-news,entertainment,Lucknow...etc")])
    fun clickOnNewsItem(
        pos: String,
        feed: String,
        source: String?,
        category: String?,
        subCategory: String?
    ) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.LIFEFEED_NEWS)
                .extra(Extra.POSITION, pos)
                .extra(Extra.FEED, feed)
                .extra(Extra.SOURCE, source ?: "")
                .extra(Extra.CATEGORY, category ?: "")
                .extra(Extra.SUB_CATEGORY, subCategory ?: "")
                .queue()
    }

    @TelemetryDoc(
            name = "Open lifefeed EC",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_EC,
            extras = [])
    @JvmStatic
    fun openLifeFeedEc() {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LIFEFEED_EC)
                .queue()
    }

    @TelemetryDoc(
            name = "Click on EC item",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_EC,
            extras = [TelemetryExtra(name = Extra.POSITION, value = "1,2,3..."),
                    TelemetryExtra(name = Extra.SOURCE, value = "bukalapak,tokopedia"),
                    TelemetryExtra(name = Extra.CATEGORY, value = "pulsa,data,game,train,flight,event")])
    fun clickOnEcItem(pos: String, source: String?, category: String?) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.LIFEFEED_EC)
                .extra(Extra.POSITION, pos)
                .extra(Extra.SOURCE, source ?: "")
                .extra(Extra.CATEGORY, category ?: "")
                .queue()
    }

    @TelemetryDoc(
            name = "Open lifefeed promo",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_PROMO,
            extras = [TelemetryExtra(name = Extra.SOURCE, value = Extra_Value.TAB + "," + Extra_Value.ARROW),
                TelemetryExtra(name = Extra.VERSION, value = LIFE_FEED_PROMO_VERSION)])
    @JvmStatic
    fun openLifeFeedPromo(source: String) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.PANEL, Value.LIFEFEED_PROMO)
                .extra(Extra.SOURCE, source)
                .extra(Extra.VERSION, LIFE_FEED_PROMO_VERSION)
                .queue()
    }

    @TelemetryDoc(
            name = "Click on promo item",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.LIFEFEED_PROMO,
            extras = [TelemetryExtra(name = Extra.POSITION, value = "1,2,3..."),
                TelemetryExtra(name = Extra.ID, value = "custom sequence number"),
                TelemetryExtra(name = Extra.FEED, value = "banner,list"),
                TelemetryExtra(name = Extra.SOURCE, value = "bukalapak,tokopedia"),
                TelemetryExtra(name = Extra.CATEGORY, value = "product,coupons"),
                TelemetryExtra(name = Extra.SUB_CATEGORY, value = "smartphones,computer,limited-time,credit-card")])
    @JvmStatic
    fun clickOnPromoItem(
        pos: String,
        id: String?,
        feed: String?,
        source: String?,
        category: String?,
        subcategory: String?
    ) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.LIFEFEED_PROMO)
                .extra(Extra.POSITION, pos)
                .extra(Extra.ID, id ?: "")
                .extra(Extra.FEED, feed ?: "")
                .extra(Extra.SOURCE, source ?: "")
                .extra(Extra.CATEGORY, category ?: "")
                .extra(Extra.SUB_CATEGORY, subcategory ?: "")
                .queue()
    }

    @TelemetryDoc(
            name = "Show File ContextMenu",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "click Rate App",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FEEDBACK,
            value = "null,dismiss,positive,negative",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "contextual_hints,settings,notification")
            ])
    @JvmStatic
    fun clickRateApp(value: String?, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.FEEDBACK, value)
                .extra(Extra.SOURCE, source).extra(Extra.VERSION, RATE_APP_NOTIFICATION_TELEMETRY_VERSION.toString())
                .queue()
        if (Value.POSITIVE == value) {
            AdjustHelper.trackEvent(EVENT_FEEDBACK_POSITIVE)
        }
    }

    @TelemetryDoc(
            name = "Show Rate App",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FEEDBACK,
            value = "",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "notification")])
    @JvmStatic
    fun showRateApp(isNotification: Boolean) {
        val builder = EventBuilder(Category.ACTION, Method.SHOW, Object.FEEDBACK)
        if (isNotification) {
            builder.extra(Extra.SOURCE, TelemetryWrapper.Extra_Value.NOTIFICATION)
        }
        builder.queue()
    }

    @TelemetryDoc(
            name = "Default Browser Notification shown",
            category = Category.ACTION,
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
            name = "Receive Firstrun Push config",
            category = Category.ACTION,
            method = Method.GET,
            `object` = Object.FIRSTRUN_PUSH,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.DELAY, value = "minutes"),
                TelemetryExtra(name = Extra.MESSAGE, value = "message")
            ])
    @JvmStatic
    fun receiveFirstrunConfig(minutes: Long, message: String?) {
        val builder = EventBuilder(Category.ACTION, Method.GET, Object.FIRSTRUN_PUSH)
                .extra(Extra.DELAY, minutes.toString())
                .extra(Extra.MESSAGE, message ?: "")
        builder.queue()
    }

    @TelemetryDoc(
            name = "Firstrun Push notification shown",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.FIRSTRUN_PUSH,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.DELAY, value = "minutes"),
                TelemetryExtra(name = Extra.MESSAGE, value = "message")
            ])
    @JvmStatic
    fun showFirstrunNotification(minutes: Long, message: String?) {
        val builder = EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN_PUSH)
                .extra(Extra.DELAY, minutes.toString())
                .extra(Extra.MESSAGE, message ?: "")
        builder.queue()
    }

    @TelemetryDoc(
            name = "Default Browser Notification Clicked",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PROMOTE_SHARE,
            value = "dismiss,share",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "contextual_hints,settings")])
    @JvmStatic
    fun promoteShareClickEvent(value: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PROMOTE_SHARE, value)
                .extra(Extra.SOURCE, source)
                .queue()

        if (Value.SHARE == value) {
            AdjustHelper.trackEvent(EVENT_SHARE_APP)
        }
    }

    @TelemetryDoc(
            name = "Promote Share Dialog shown",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "Home Impression",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.HOME,
            value = "",
            extras = [])
    @JvmStatic
    fun showHome() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.HOME)
                .queue()
    }

    @TelemetryDoc(
            name = "Banner Impression: new",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.BANNER,
            value = Value.NEW,
            extras = [TelemetryExtra(name = Extra.TO, value = "banner page id")])
    @JvmStatic
    fun showBannerNew(id: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.BANNER, Value.NEW)
                .extra(Extra.TO, id)
                .queue()
    }

    @TelemetryDoc(
            name = "Banner Impression: update",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.BANNER,
            value = Value.UPDATE,
            extras = [TelemetryExtra(name = Extra.TO, value = "banner page id")])
    @JvmStatic
    fun showBannerUpdate(id: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.BANNER, Value.UPDATE)
                .extra(Extra.TO, id)
                .queue()
    }

    @TelemetryDoc(
            name = "Banner Impression: return",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.BANNER,
            value = Value.RETURN,
            extras = [TelemetryExtra(name = Extra.TO, value = "banner page id")])
    @JvmStatic
    fun showBannerReturn(id: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.BANNER, Value.RETURN)
                .extra(Extra.TO, id)
                .queue()
    }

    @TelemetryDoc(
            name = "Banner Impression: swipe",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.BANNER,
            value = Value.SWIPE,
            extras = [TelemetryExtra(name = Extra.TO, value = "banner page id")])
    @JvmStatic
    fun showBannerSwipe(id: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.BANNER, Value.SWIPE)
                .extra(Extra.TO, id)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Banner Background",
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            category = Category.ACTION,
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
            name = "Click VPN Survey",
            category = Category.ACTION,
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
            name = "Survey Result",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.DOORHANGER,
            value = "negative,positive,dismiss",
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "vpn,wifi_finder")])
    @JvmStatic
    fun surveyResult(result: String, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DOORHANGER, result)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Show VPN Recommend",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.HOME,
            value = Value.VPN_RECOMMEND,
            extras = [])
    @JvmStatic
    fun showVpnRecommender(installed: Boolean) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.HOME, Value.VPN_RECOMMEND)
                .extra(Extra.VPN_INSTALLED, java.lang.Boolean.toString(installed))
                .queue()
    }

    @TelemetryDoc(
            name = "Click VPN Recommend",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.HOME,
            value = Value.VPN_RECOMMEND,
            extras = [])
    @JvmStatic
    fun clickVpnRecommender(installed: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.HOME, Value.VPN_RECOMMEND)
                .extra(Extra.VPN_INSTALLED, java.lang.Boolean.toString(installed))
                .queue()
    }

    @TelemetryDoc(
            name = "Click VPN Recommend",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.VPN_DOORHANGER,
            value = "negative,positive",
            extras = [])
    @JvmStatic
    fun clickVpnRecommend(positive: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.VPN_DOORHANGER, if (positive) Value.POSITIVE else Value.NEGATIVE)
                .queue()

        if (positive) {
            AdjustHelper.trackEvent(EVENT_GET_VPN)
        }
    }

    @TelemetryDoc(
            name = "Dismiss VPN Recommend",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.VPN_DOORHANGER,
            value = Value.DISMISS,
            extras = [])
    @JvmStatic
    fun dismissVpnRecommend() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.VPN_DOORHANGER, Value.DISMISS)
                .queue()
    }

    @JvmStatic
    fun findInPage(type: FIND_IN_PAGE) {
        val builder = when (type) {

            OPEN_BY_MENU -> clickMenuFindInPage()
            CLICK_PREVIOUS -> clickFindInPagePrevious()
            CLICK_NEXT -> clickFindInPageNext()
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
            name = "Long Press Toolbar Download Indicator",
            category = Category.EVENT_DOWNLOADS,
            method = Method.LONG_PRESS,
            `object` = Object.TOOLBAR,
            value = Value.DOWNLOAD,
            extras = [])
    @JvmStatic
    fun longPressDownloadIndicator() {
        EventBuilder(Category.EVENT_DOWNLOADS, Method.LONG_PRESS, Object.TOOLBAR, Value.DOWNLOAD)
                .queue()
    }

    @TelemetryDoc(
            name = "Click FindInPage Next",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FIND_IN_PAGE,
            value = Value.NEXT,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickFindInPageNext() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.NEXT)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Click FindInPage Previous",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.FIND_IN_PAGE,
            value = Value.PREVIOUS,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickFindInPagePrevious() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.FIND_IN_PAGE, Value.PREVIOUS)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @TelemetryDoc(
            name = "Click Menu FindInPage",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.FIND_IN_PAGE,
            extras = [TelemetryExtra(name = Extra.VERSION, value = "2")])
    internal fun clickMenuFindInPage() =
            EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.FIND_IN_PAGE)
                    .extra(Extra.VERSION, Integer.toString(FIND_IN_PAGE_VERSION))

    @JvmStatic
    fun clickDefaultBrowserInSetting() {
        AdjustHelper.trackEvent(EVENT_SET_DEFAULT_BROWSER)
    }

    @TelemetryDoc(
            name = "Click Quick Search",
            category = Category.SEARCH,
            method = Method.CLICK,
            `object` = Object.QUICK_SEARCH,
            value = "",
            extras = [TelemetryExtra(name = Extra.ENGINE, value = "Facebook,Youtube,Instagram")])
    @JvmStatic
    fun clickQuickSearchEngine(engineName: String) {
        EventBuilder(Category.SEARCH, Method.CLICK, Object.QUICK_SEARCH, null)
                .extra(Extra.ENGINE, engineName).queue()
    }

    @TelemetryDoc(
            name = "Enter Landscape Mode",
            category = Category.ENTER_LANDSCAPE_MODE,
            method = Method.CHANGE,
            `object` = Object.LANDSCAPE_MODE,
            value = Value.ENTER,
            extras = [])
    @JvmStatic
    fun enterLandscapeMode() {
        EventBuilder(Category.ENTER_LANDSCAPE_MODE, Method.CHANGE, Object.LANDSCAPE_MODE, Value.ENTER).queue()
    }

    @TelemetryDoc(
            name = "Exit Landscape Mode",
            category = Category.ENTER_LANDSCAPE_MODE,
            method = Method.CHANGE,
            `object` = Object.LANDSCAPE_MODE,
            value = Value.EXIT,
            extras = [TelemetryExtra(name = Extra.DURATION, value = "duration in ms")])
    @JvmStatic
    fun exitLandscapeMode(duration: Long) {
        EventBuilder(Category.ENTER_LANDSCAPE_MODE, Method.CHANGE, Object.LANDSCAPE_MODE, Value.EXIT)
                .extra(Extra.DURATION, duration.toString()).queue()
    }

    internal class EventBuilder @JvmOverloads constructor(
        category: String,
        method: String,
        `object`: String?,
        value: String? = null
    ) {
        var telemetryEvent: TelemetryEvent
        var firebaseEvent: FirebaseEvent

        init {
            lazyInit()
            Log.d(TAG, "EVENT:$category/$method/$`object`/$value")

            telemetryEvent = TelemetryEvent.create(category, method, `object`, value)
            firebaseEvent = FirebaseEvent.create(category, method, `object`, value)
        }

        fun extra(key: String, value: String): EventBuilder {
            Log.d(TAG, "EXTRA:$key/$value")
            telemetryEvent.extra(key, value)
            firebaseEvent.param(key, value)
            return this
        }

        fun queue(sendNow: Boolean = false) {

            val context = TelemetryHolder.get().configuration.context
            if (context != null) {
                if (sendNow) {
                    // if the user open MainActivity and goes to Setting Fragment and disable "Send Usage Data", we still want to send that event asap
                    // This pref is wrapped in async call so we don't know when to set it back.
                    // It'll resume to it's default value when the user start the app next time.
                    TelemetryHolder.get().configuration.minimumEventsForUpload = 1
                    sendEventNow(telemetryEvent)
                } else {
                    telemetryEvent.queue()
                }

                firebaseEvent.event(context)
            }
        }

        private fun sendEventNow(event: TelemetryEvent) {
            val telemetry = TelemetryHolder.get()
            var focusEventBuilder: TelemetryPingBuilder? = null
            // we only have FocusEventPing now
            for (telemetryPingBuilder in telemetry.builders) {
                if (telemetryPingBuilder is TelemetryEventPingBuilder) {
                    focusEventBuilder = telemetryPingBuilder
                }
            }
            val measurement: EventsMeasurement
            val addedPingType: String

            if (focusEventBuilder == null) {
                throw IllegalStateException("Expect either TelemetryEventPingBuilder or TelemetryMobileEventPingBuilder to be added to queue events")
            }

            measurement = (focusEventBuilder as TelemetryEventPingBuilder).eventsMeasurement
            addedPingType = focusEventBuilder.type

            measurement.add(event)
            telemetry.queuePing(addedPingType).scheduleUpload()
        }

        companion object {

            const val TAG = "TelemetryWrapper"

            fun lazyInit() {

                if (FirebaseEvent.isInitialized()) {
                    return
                }
                val context = TelemetryHolder.get().configuration.context ?: return
                val prefKeyWhitelist = HashMap<String, String>()
                prefKeyWhitelist[context.getString(R.string.pref_key_search_engine)] = "search_engine"
                prefKeyWhitelist[context.getString(R.string.pref_s_news)] = "pref_s_news"

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

        internal fun addCustomPing(
            configuration: TelemetryConfiguration,
            measurement: TelemetryMeasurement
        ) {
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
            private const val MEASUREMENT_CAPTURE_COUNT = "capture_count"
        }
    }
}
