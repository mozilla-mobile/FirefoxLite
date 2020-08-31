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
import android.net.ConnectivityManager
import android.os.StrictMode.ThreadPolicy.Builder
import android.preference.PreferenceManager
import android.util.Log
import android.webkit.PermissionRequest
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_NEXT
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.CLICK_PREVIOUS
import org.mozilla.focus.telemetry.TelemetryWrapper.FIND_IN_PAGE.OPEN_BY_MENU
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.SETTINGS
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.content.common.data.TabSwipeTelemetryData
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.strictmodeviolator.StrictModeViolation
import org.mozilla.telemetry.annotation.TelemetryDoc
import org.mozilla.telemetry.annotation.TelemetryExtra
import kotlin.math.roundToInt

object TelemetryWrapper {
    private const val RATE_APP_NOTIFICATION_TELEMETRY_VERSION = 3
    private const val DEFAULT_BROWSER_NOTIFICATION_TELEMETRY_VERSION = 2
    private const val OPEN_HOME_LINK_VERSION = "2"
    private const val FIND_IN_PAGE_VERSION = 2
    private const val SEARCHCLEAR_TELEMETRY_VERSION = "2"
    private const val SEARCHDISMISS_TELEMETRY_VERSION = "2"
    private lateinit var appContext: Context

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
        const val RELOAD = "reload"
        const val START = "start"
        const val END = "end"
        const val IMPRESSION = "impression"
        const val PIN = "pin"
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
        const val SIGN_IN = "sign_in"
        const val SHOW_KEYBOARD = "show_keyboard"
        const val START_TYPING = "start_typing"
    }

    internal object Object {
        const val PRIVATE_MODE = "private_mode"
        const val PRIVATE_SHORTCUT = "private_shortcut"
        const val PANEL = "panel"
        const val TOOLBAR = "toolbar"
        const val DRAWER = "drawer"
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
        const val FIRSTRUN_PUSH = "firstrun_push"
        const val FEEDBACK = "feedback"
        const val DEFAULT_BROWSER = "default_browser"
        const val PROMOTE_SHARE = "promote_share"
        const val THEMETOY = "themetoy"
        const val QUICK_SEARCH = "quicksearch"
        const val LANDSCAPE_MODE = "landscape_mode"
        const val UPDATE_MESSAGE = "update_msg"
        const val UPDATE = "update"
        const val ONBOARDING = "onboarding"
        const val CONTEXTUAL_HINT = "contextual_hint"
        const val CONTENT_HUB = "content_hub"
        const val CONTENT_HOME = "content_home"
        const val CONTENT_TAB = "content_tab"
        const val TAB_SWIPE = "tab_swipe"
        const val LOGOMAN = "logoman"
        const val NOTIFICATION = "notification"
        const val MESSAGE = "message"
        const val CATEGORY = "category"
        const val PROCESS = "process"
        const val CHALLENGE_PAGE = "challenge_page"
        const val TASK = "task"
        const val ACCOUNT = "account"
        const val REDEEM_PAGE = "redeem_page"
        const val PROFILE = "profile"
        const val DETAIL_PAGE = "detail_page"
        const val TOAST = "toast"
        const val SNACKBAR = "snackbar"
        const val DOWNLOAD = "download"
    }

    object Value {
        internal const val HOME = "home"
        internal const val TOPSITE = "top_site"
        internal const val DOWNLOAD = "download"
        internal const val DOWNLOADED = "downloaded"
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
        internal const val THEME = "theme"
        internal const val ADD_TOPSITE = "add_topsite"
        internal const val SEARCH_BUTTON = "search_btn"
        internal const val SEARCH_BOX = "search_box"
        internal const val MINI_URLBAR = "mini_urlbar"
        internal const val FILE = "file"
        internal const val IMAGE = "image"
        internal const val LINK = "link"
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
        internal const val LAUNCHER = "launcher"
        internal const val EXTERNAL_APP = "external_app"
        internal const val SHORTCUT = "shortcut"
        internal const val PRIVATE_MODE = "private_mode"
        internal const val PREVIOUS = "previous"
        internal const val NEXT = "next"
        internal const val NIGHT_MODE = "night_mode"
        internal const val NIGHT_MODE_BRIGHTNESS = "night_mode_brightness"
        internal const val SETTINGS_PRIVATE_SHORTCUT = "pref_private_shortcut"
        internal const val APPLY = "apply"
        internal const val FIRSTRUN = "firstrun"
        internal const val IN_APP_MESSAGE = "in_app_message"
        internal const val VERTICAL = "vertical"
        internal const val TAB_SWIPE = "tab_swipe"
        internal const val OPEN_IN_BROWSER = "OPEN_IN_BROWSER"
        internal const val BACK = "back"
        internal const val JOIN = "join"
        internal const val TASK = "task"
        internal const val CHALLENGE_COMPLETE = "challenge_complete"
        internal const val CODE = "code"
        internal const val USE = "use"
        internal const val REWARD = "reward"
        internal const val ITEM = "item"
        internal const val LOGIN = "login"
        internal const val CONTEXTMENU = "contextmenu"
        internal const val CONTENT_HOME = "content_home"
        internal const val UPDATE = "update"
        internal const val MORE = "more"
        internal const val DETAIL_PAGE = "detail_page"
        internal const val TRAVEL_SEARCH_RESULT = "travel_search_result"
        internal const val SET_DEFAULT_TRAVEL_SEARCH = "setdefault_travel_search"
        internal const val PERSONALIZATION = "personalization"
        internal const val LANGUAGE = "language"
        internal const val LIFESTYLE = "lifestyle"
        internal const val CATEGORY = "category"
        internal const val EXIT_WARNING = "exit_warning"
        internal const val SET_DEFAULT_BY_SETTINGS = "set_default_by_settings"
        internal const val SET_DEFAULT_BY_LINK = "set_default_by_link"
        internal const val SET_DEFAULT_SUCCESS = "set_default_success"
        internal const val SET_DEFAULT_TRY_AGAIN = "set_default_try_again"
        internal const val GO_SET_DEFAULT = "go_set_default"
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
        const val CATEGORY = "category"
        // Remove the last character cause Telemetry library will do that for you.( > 15chars)
        const val CATEGORY_VERSION = "category_versio"
        const val ENGINE = "engine"
        const val DELAY = "delay"
        const val MESSAGE_ID = "message_id"
        const val POSITION = "position"
        const val FEED = "feed"
        const val SUB_CATEGORY_ID = "subcategory_id"
        const val VERSION_ID = "version_id"
        const val MODE = "mode"
        const val COMPONENT_ID = "component_id"
        const val DURATION = "duration"
        const val FROM_BUILD = "from_build"
        const val TO_BUILD = "to_build"
        const val ACTION = "action"
        const val PAGE = "page"
        const val FINISH = "finish"
        const val VERTICAL = "vertical"
        const val LINK = "link"
        const val PRIMARY = "primary"
        const val SESSION_TIME = "session_time"
        const val URL_COUNTS = "url_counts"
        const val APP_LINK = "app_link"
        const val SHOW_KEYBOARD = "show_keyboard"
        const val IMPRESSION = "impression"
        const val LOADTIME = "loadtime"
        const val TASK = "task"
        const val FINISHED = "finished"
        const val ITEM_ID = "item_id"
        const val ITEM_NAME = "item_name"
        const val BACKGROUND = "background"
        const val CHALLENGE_NAME = "challenge_name"
        const val BUTTON_TEXT = "button_text"
        const val PERSONALIZATION = "personalization"
        const val LANGUAGE = "language"
        const val KEYWORD = "keyword"
        const val ORIENTATION = "orientation"
        const val PINNED = "pinned"
        const val INTEREST = "interest"
        const val THEME = "theme"
        const val DOWNLOAD_ID = "download_id"
        const val FILE_SIZE = "file_size"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val SUPPORT_RESUME = "support_resume"
        const val PROGRESS = "progress"
        const val VALID_SSL = "valid_ssl"
        const val NETWORK = "network"
        const val STATUS = "status"
        const val REASON = "reason"
    }

    object Extra_Value {
        const val SETTING = "settings"
        const val CONTEXTUAL_HINTS = "contextual_hints"
        const val NOTIFICATION = "notification"
        internal const val WEB_SEARCH = "web_search"
        internal const val TEXT_SELECTION = "text_selection"
        internal const val DEFAULT = "default"
        internal const val WEBVIEW = "webview"
        internal const val MENU = "menu"
        internal const val PRIVATE_MODE = "private_mode"
        internal const val SYSTEM_BACK = "system_back"
        const val LAUNCHER = "launcher"
        const val EXTERNAL_APP = "external_app"
        const val DISMISS = "dismiss"
        const val FORCE_CLOSE = "force_close"
        const val SNACKBAR = "snackbar"
        const val NEW = "new"
        const val REMINDER = "reminder"
        const val SHOPPING = "shopping"
        const val TRAVEL = "travel"
        const val LIFESTYLE = "lifestyle"
        const val REWARDS = "rewards"
        const val WEATHER = "weather"
        const val ALL = "all"
        const val URL = "url"
        const val DEEPLINK = "deeplink"
        const val OPEN = "open"
        const val INSTALL = "install"
        const val LATER = "later"
        const val LOGIN = "login"
        const val CLOSE = "close"
        const val MISSION = "mission"
        const val GIFT = "gift"
        const val HOME = "home"
        const val TAB_SWIPE = "tab_swipe"
        const val EXPLORE = "explore"
        const val BUCKET_LIST = "bucket_list"
        const val GOOGLE = "google"
        const val BOOKING_COM = "booking.com"
        const val UPDATE = "update"
        const val SAVE = "save"
        const val REMOVE = "remove"
        const val TRAVEL_DISCOVERY = "travel_discovery"
        const val GOOGLE_SEARCH = "google_search"
        const val SET_DEFAULT = "setdefault"
        const val PORTRAIT = "portrait"
        const val LANDSCAPE = "landscape"
        const val OK = "ok"
        const val CANCEL = "cancel"
        const val TRY_AGAIN = "try_again"
        const val CONTEXT_MENU = "context_menu"
        const val EMPTY_HINT = "empty_hint"
        const val AWESOMEBAR_TYPE_HISTORY = "history"
        const val AWESOMEBAR_TYPE_BOOKMARK = "bookmark"
        const val AWESOMEBAR_TYPE_TABTRAY = "tabtray"
        const val AWESOMEBAR_TYPE_CLIPBOARD = "clipboard"
        const val AWESOMEBAR_TYPE_MANUALCOMPLETE = "manualcomplete"
        const val AWESOMEBAR_TYPE_AUTOCOMPLETE = "autocomplete"
        const val AWESOMEBAR_TYPE_SUGGESTION = "suggestion"
        const val CONTENT_PREF_DEFAULT = "DEFAULT"
        const val CONTENT_PREF_DEALS = "DEALS"
        const val CONTENT_PREF_NEWS = "NEWS"
        const val CONTENT_PREF_ENTERTAINMENT = "ENTERTAINMENT"
    }

    enum class FIND_IN_PAGE {
        OPEN_BY_MENU,
        CLICK_PREVIOUS,
        CLICK_NEXT
    }

    // context passed here is nullable cause it may come from Java code
    @JvmStatic
    fun isTelemetryEnabled(context: Context?): Boolean {
        if (context == null) return false
        // The first access to shared preferences will require a disk read.
        return StrictModeViolation.tempGrant({ obj: Builder -> obj.permitDiskReads() }, {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isEnabledByDefault = AppConstants.isBuiltWithFirebase()
            // Telemetry is not enable by default in debug build. But the user / developer can choose to turn it on
            // in AndroidTest, this is enabled by default
            preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), isEnabledByDefault)
        })
    }

    @JvmStatic
    fun setTelemetryEnabled(context: Context, enabled: Boolean) {
        val resources = context.resources
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val key = resources.getString(R.string.pref_key_telemetry)
        preferences.edit()
                .putBoolean(key, enabled)
                .apply()
    }

    fun init(context: Context) {
        StrictModeViolation.tempGrant({ obj: Builder -> obj.permitDiskReads().permitDiskWrites() }) {
            appContext = context.applicationContext
            // When initializing the telemetry library it will make sure that all directories exist and
            // are readable/writable.
            val telemetryEnabled = isTelemetryEnabled(context)
            FirebaseHelper.init(context, telemetryEnabled)
        }
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
            value = "${Value.POSITIVE},${Value.NEGATIVE},${Value.DISMISS}",
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
            extras = [TelemetryExtra(name = Extra.TO, value = "New Value for the pref")],
            skipAmplitude = true)
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
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SETTING,
            value = "settings pref key",
            extras = [],
            skipAmplitude = true)
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
            extras = [TelemetryExtra(name = Extra.SOURCE, value = "TurboMode")])
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
        EventBuilder(Category.ACTION, Method.BACKGROUND, Object.APP).queue()
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
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun showTabTrayToolbar(mode: String, position: Int, isInLandscape: Boolean = false) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TABTRAY, Value.TOOLBAR)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
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
            name = "Click Menu - Theme",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.THEME,
            extras = [])
    @JvmStatic
    fun clickMenuTheme() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.THEME).queue()
    }

    @TelemetryDoc(
            name = "Click Menu - Add Topsite",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MENU,
            value = Value.ADD_TOPSITE,
            extras = [])
    @JvmStatic
    fun clickMenuAddTopsite() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MENU, Value.ADD_TOPSITE).queue()
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
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickToolbarReload(mode: String, position: Int, isInLandscape: Boolean = false) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
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
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickToolbarShare(mode: String, position: Int, isInLandscape: Boolean = false) {
        EventBuilder(Category.ACTION, Method.SHARE, Object.TOOLBAR, Value.LINK)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
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
    }

    @TelemetryDoc(
            name = "Click Menu - Pin shortcut",
            category = Category.ACTION,
            method = Method.PIN_SHORTCUT,
            `object` = Object.MENU,
            value = Value.LINK,
            extras = [])
    @JvmStatic
    fun clickMenuPinShortcut() {
        EventBuilder(Category.ACTION, Method.PIN_SHORTCUT, Object.MENU, Value.LINK)
                .queue()
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
    }

    @TelemetryDoc(
            name = "Click Top Site",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [TelemetryExtra(name = Extra.ON, value = "Top Site Position"),
                TelemetryExtra(name = Extra.SOURCE, value = "Preset Top Site like **"),
                TelemetryExtra(name = Extra.VERSION, value = OPEN_HOME_LINK_VERSION),
                TelemetryExtra(name = Extra.DEFAULT, value = "true,false"),
                TelemetryExtra(name = Extra.PINNED, value = "true,false")
            ])
    @JvmStatic
    fun clickTopSiteOn(index: Int, source: String, isDefault: Boolean, isPinned: Boolean) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.HOME, Value.LINK)
                .extra(Extra.ON, index.toString())
                .extra(Extra.SOURCE, source)
                .extra(Extra.VERSION, OPEN_HOME_LINK_VERSION)
                .extra(Extra.DEFAULT, isDefault.toString())
                .extra(Extra.PINNED, isPinned.toString())
                .queue()

        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOPSITE)
                .queue()
    }

    @TelemetryDoc(
            name = "Remove Top Site",
            category = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.DEFAULT, value = "true,false"),
                TelemetryExtra(name = Extra.ON, value = "Default Top Site Position"),
                TelemetryExtra(name = Extra.SOURCE, value = "Default Topsite Name"),
                TelemetryExtra(name = Extra.PINNED, value = "true,false")
            ])
    @JvmStatic
    fun removeTopSite(isDefault: Boolean, position: Int, source: String, isPinned: Boolean) {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.HOME, Value.LINK)
                .extra(Extra.DEFAULT, isDefault.toString())
                .extra(Extra.ON, position.toString())
                .extra(Extra.SOURCE, source)
                .extra(Extra.PINNED, isPinned.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Add Topsite",
            category = Category.ACTION,
            method = Method.ADD,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.FROM, value = "${Extra_Value.CONTEXT_MENU},${Extra_Value.EMPTY_HINT}")
            ])
    @JvmStatic
    fun addTopSite(from: String) {
        EventBuilder(Category.ACTION, Method.ADD, Object.HOME, Value.LINK)
                .extra(Extra.FROM, from)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Add Topsite Snackbar",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SNACKBAR,
            value = Value.ADD_TOPSITE,
            extras = [])
    @JvmStatic
    fun clickAddTopSiteFromSnackBar() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SNACKBAR, Value.ADD_TOPSITE)
                .queue()
    }

    @TelemetryDoc(
            name = "Select to Add Topsite",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PANEL,
            value = Value.ADD_TOPSITE,
            extras = [
                TelemetryExtra(name = Extra.DEFAULT, value = "true,false"),
                TelemetryExtra(name = Extra.ON, value = "Default Top Site Position"),
                TelemetryExtra(name = Extra.SOURCE, value = "Default Topsite Name")
            ])
    @JvmStatic
    fun selectToAddTopSite(isDefault: Boolean, position: Int, source: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PANEL, Value.ADD_TOPSITE)
                .extra(Extra.DEFAULT, java.lang.Boolean.toString(isDefault))
                .extra(Extra.ON, position.toString())
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Theme Contextual Hint",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.CONTEXTUAL_HINT,
            value = Value.THEME,
            extras = [])
    @JvmStatic
    fun showThemeContextualHint() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.CONTEXTUAL_HINT, Value.THEME).queue()
    }

    @TelemetryDoc(
            name = "Click Theme Contextual Hint",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CONTEXTUAL_HINT,
            value = Value.THEME,
            extras = [
                TelemetryExtra(name = Extra.THEME, value = "aqua,cyan,raspberry,iris,night")
            ])
    @JvmStatic
    fun clickThemeContextualHint(name: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CONTEXTUAL_HINT, Value.THEME)
                .extra(Extra.THEME, name)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Go-Set-Default Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.GO_SET_DEFAULT,
            extras = [])
    @JvmStatic
    fun showGoSetDefaultMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.GO_SET_DEFAULT).queue()
    }

    @TelemetryDoc(
            name = "Click Go-Set-Default Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.GO_SET_DEFAULT,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "later,ok")
            ])
    @JvmStatic
    fun clickGoSetDefaultMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.GO_SET_DEFAULT)
                .extra(Extra.ACTION, action)
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
    fun urlBarEvent(
        isUrl: Boolean,
        isSuggestion: Boolean,
        isInLandscape: Boolean,
        type: String
    ) {
        if (isUrl) {
            TelemetryWrapper.browseEvent(isInLandscape, type)
        } else if (isSuggestion) {
            TelemetryWrapper.searchSelectEvent(isInLandscape)
        } else {
            TelemetryWrapper.searchEnterEvent(isInLandscape)
        }
    }

    @TelemetryDoc(
            name = "Enter an url in SearchBar",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.SEARCH_BAR,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape"),
                TelemetryExtra(
                    name = Extra.TYPE,
                    value = "history,bookmark,clipboard,suggestion,tabtray,manualcomplete,autocomplete"
                )
            ])
    private fun browseEvent(isInLandscape: Boolean, type: String) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.SEARCH_BAR, Value.LINK)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .extra(Extra.TYPE, type)
                .queue()
    }

    @TelemetryDoc(
            name = "Use SearchSuggestion SearchBar",
            category = Category.ACTION,
            method = Method.TYPE_SELECT_QUERY,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun searchSelectEvent(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Search with text in SearchBar",
            category = Category.ACTION,
            method = Method.TYPE_QUERY,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    private fun searchEnterEvent(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
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
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = SEARCHCLEAR_TELEMETRY_VERSION),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun searchClear(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.CLEAR, Object.SEARCH_BAR)
                .extra(Extra.VERSION, SEARCHCLEAR_TELEMETRY_VERSION)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss SearchBar",
            category = Category.ACTION,
            method = Method.CANCEL,
            `object` = Object.SEARCH_BAR,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERSION, value = SEARCHDISMISS_TELEMETRY_VERSION),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun searchDismiss(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.CANCEL, Object.SEARCH_BAR)
                .extra(Extra.VERSION, SEARCHDISMISS_TELEMETRY_VERSION)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
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
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickUrlbar(vertical: String, isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.MINI_URLBAR)
                .apply {
                    if (vertical.isNotEmpty()) {
                        extra(Extra.VERTICAL, vertical)
                    }
                }
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
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
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickToolbarSearch(mode: String, position: Int, isInLandscape: Boolean = false) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.SEARCH_BUTTON)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
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
                TelemetryExtra(name = Extra.POSITION, value = "[0-4]"),
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickAddTabToolbar(mode: String, position: Int, isInLandscape: Boolean = false) {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TOOLBAR)
                .extra(Extra.VERSION, "2")
                .extra(Extra.MODE, mode)
                .extra(Extra.POSITION, Integer.toString(position))
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Add Tab from TabTray",
            category = Category.ACTION,
            method = Method.ADD,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickAddTabTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Enter Private Mode from TabTray",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PRIVATE_MODE,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun privateModeTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PRIVATE_MODE, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Switch Tab From TabTray",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun clickTabFromTabTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.TAB, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Remove Tab From TabTray",
            category = Category.ACTION,
            method = Method.REMOVE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun closeTabFromTabTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.REMOVE, Object.TAB, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Swipe Tab From TabTray",
            category = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.TAB,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun swipeTabFromTabTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.TAB, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
    }

    @TelemetryDoc(
            name = "Close all From TabTray",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CLOSE_ALL,
            value = Value.TABTRAY,
            extras = [
                TelemetryExtra(name = Extra.ORIENTATION, value = "portrait,landscape")
            ])
    @JvmStatic
    fun closeAllTabFromTabTray(isInLandscape: Boolean) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CLOSE_ALL, Value.TABTRAY)
                .extra(Extra.ORIENTATION, if (isInLandscape) Extra_Value.LANDSCAPE else Extra_Value.PORTRAIT)
                .queue()
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
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message_id"),
                TelemetryExtra(name = Extra.LINK, value = "url|deeplink|null"),
                TelemetryExtra(name = Extra.BACKGROUND, value = "true|false")
            ])
    @JvmStatic
    fun receiveFirstrunConfig(minutes: Long, messageId: String, link: String?, background: Boolean) {
        val builder = EventBuilder(Category.ACTION, Method.GET, Object.FIRSTRUN_PUSH)
                .extra(Extra.DELAY, minutes.toString())
                .extra(Extra.MESSAGE_ID, messageId)
                .extra(Extra.LINK, link ?: "")
                .extra(Extra.BACKGROUND, background.toString())
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
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message_id"),
                TelemetryExtra(name = Extra.LINK, value = "url|deeplink|null"),
                TelemetryExtra(name = Extra.BACKGROUND, value = "true|false")
            ])
    @JvmStatic
    fun showFirstrunNotification(minutes: Long, messageId: String, link: String?, background: Boolean) {
        val builder = EventBuilder(Category.ACTION, Method.SHOW, Object.FIRSTRUN_PUSH)
                .extra(Extra.DELAY, minutes.toString())
                .extra(Extra.MESSAGE_ID, messageId)
                .extra(Extra.LINK, link ?: "")
                .extra(Extra.BACKGROUND, background.toString())
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

    @TelemetryDoc(
            name = "Show in-app update intro dialog",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.UPDATE_MESSAGE,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version")
            ])
    fun showInAppUpdateIntroDialog(toVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.UPDATE_MESSAGE)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Show google play's in-app update dialog",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.UPDATE,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version")
            ])
    fun showInAppUpdateGooglePlayDialog(toVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.UPDATE)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Click in-app update intro dialog",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.UPDATE_MESSAGE,
            value = "${Value.POSITIVE},${Value.NEGATIVE}",
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version"),
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.DISMISS},${Extra_Value.FORCE_CLOSE}")
            ])
    fun clickInAppUpdateIntroDialog(value: String, isForceClose: Boolean, toVersion: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.UPDATE_MESSAGE, value)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .extra(Extra.ACTION, if (isForceClose) {
                    Extra_Value.FORCE_CLOSE
                } else {
                    Extra_Value.DISMISS
                })
                .queue()
    }

    @TelemetryDoc(
            name = "Click google play's in-app update dialog",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.UPDATE,
            value = "${Value.POSITIVE},${Value.NEGATIVE}",
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version"),
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.DISMISS},${Extra_Value.FORCE_CLOSE}")
            ])
    fun clickInAppUpdateGooglePlayDialog(value: String, isForceClose: Boolean, toVersion: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.UPDATE, value)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .extra(Extra.ACTION, if (isForceClose) {
                    Extra_Value.FORCE_CLOSE
                } else {
                    Extra_Value.DISMISS
                })
                .queue()
    }

    @TelemetryDoc(
            name = "Show in-app update install prompt",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.UPDATE,
            value = Value.DOWNLOADED,
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version"),
                TelemetryExtra(name = Extra.TYPE, value = "${Extra_Value.NEW},${Extra_Value.REMINDER}")
            ])
    fun showInAppUpdateInstallPrompt(type: String, toVersion: Int) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.UPDATE, Value.DOWNLOADED)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .extra(Extra.TYPE, type)
                .queue()
    }

    @TelemetryDoc(
            name = "Click in-app update install prompt",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.UPDATE,
            value = Value.APPLY,
            extras = [
                TelemetryExtra(name = Extra.FROM_BUILD, value = "old version"),
                TelemetryExtra(name = Extra.TO_BUILD, value = "new version"),
                TelemetryExtra(
                        name = Extra.SOURCE,
                        value = "${Extra_Value.NOTIFICATION},${Extra_Value.SNACKBAR}"
                ),
                TelemetryExtra(name = Extra.TYPE, value = "${Extra_Value.NEW},${Extra_Value.REMINDER}")
            ])
    fun clickInAppUpdateInstallPrompt(source: String, type: String, toVersion: Int) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.UPDATE, Value.APPLY)
                .extra(Extra.FROM_BUILD, BuildConfig.VERSION_CODE.toString())
                .extra(Extra.TO_BUILD, toVersion.toString())
                .extra(Extra.SOURCE, source)
                .extra(Extra.TYPE, type)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Firstrun Onboarding",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.ONBOARDING,
            value = Value.FIRSTRUN,
            extras = [])
    fun showFirstRunOnBoarding() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.ONBOARDING, Value.FIRSTRUN).queue()
    }

    @TelemetryDoc(
            name = "Click Firstrun Onboarding",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.ONBOARDING,
            value = Value.FIRSTRUN,
            extras = [
                TelemetryExtra(name = Extra.ON, value = "time spent on page"),
                TelemetryExtra(name = Extra.PAGE, value = "[0-9]"),
                TelemetryExtra(name = Extra.FINISH, value = "true,false"),
                TelemetryExtra(name = Extra.INTEREST, value = "default|deals||news|entertainment")
            ])
    fun clickFirstRunOnBoarding(timeSpent: Long, pageIndex: Int, finish: Boolean, interest: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.ONBOARDING, Value.FIRSTRUN)
                .extra(Extra.ON, timeSpent.toString())
                .extra(Extra.PAGE, pageIndex.toString())
                .extra(Extra.FINISH, finish.toString())
                .extra(Extra.INTEREST, interest)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Logoman",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.LOGOMAN,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.TYPE, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS},${Extra_Value.WEATHER},null"),
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message id")
            ])
    fun showLogoman(type: String?, link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.LOGOMAN)
                .extra(Extra.TYPE, type ?: "null")
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Click Logoman",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.LOGOMAN,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.TYPE, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS},${Extra_Value.WEATHER},null"),
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message id")
            ])
    fun clickLogoman(type: String?, link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.LOGOMAN)
                .extra(Extra.TYPE, type ?: "null")
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Swipe Logoman",
            category = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.LOGOMAN,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.TYPE, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS},${Extra_Value.WEATHER},null"),
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message id")
            ])
    fun swipeLogoman(type: String?, link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.LOGOMAN)
                .extra(Extra.TYPE, type ?: "null")
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Get Notification",
            category = Category.ACTION,
            method = Method.GET,
            `object` = Object.NOTIFICATION,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "messageId")
            ])
    @JvmStatic
    fun getNotification(link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.GET, Object.NOTIFICATION)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Show Notification",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.NOTIFICATION,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "messageId")
            ])
    @JvmStatic
    fun showNotification(link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.NOTIFICATION)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss Notification",
            category = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.NOTIFICATION,
            value = Value.DISMISS,
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "messageId")
            ])
    @JvmStatic
    fun dismissNotification(link: String?, messageId: String?) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.NOTIFICATION, Value.DISMISS)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Dismiss D1 Notification",
            category = Category.ACTION,
            method = Method.SWIPE,
            `object` = Object.FIRSTRUN_PUSH,
            value = Value.DISMISS,
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message_id"),
                TelemetryExtra(name = Extra.BACKGROUND, value = "true|false")
            ])
    @JvmStatic
    fun dismissFirstrunNotification(link: String?, messageId: String, background: Boolean) {
        EventBuilder(Category.ACTION, Method.SWIPE, Object.FIRSTRUN_PUSH, Value.DISMISS)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId)
                .extra(Extra.BACKGROUND, background.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Open Notification",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.NOTIFICATION,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "messageId"),
                TelemetryExtra(name = Extra.BACKGROUND, value = "true|false")
            ])
    @JvmStatic
    fun openNotification(link: String?, messageId: String?, background: Boolean) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.NOTIFICATION)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId ?: "null")
                .extra(Extra.BACKGROUND, background.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Open D1 Notification",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.FIRSTRUN_PUSH,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.LINK, value = "${Extra_Value.URL},${Extra_Value.DEEPLINK},null"),
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "message_id"),
                TelemetryExtra(name = Extra.BACKGROUND, value = "true|false")
            ])
    @JvmStatic
    fun openD1Notification(link: String?, messageId: String, background: Boolean) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.FIRSTRUN_PUSH)
                .extra(Extra.LINK, link ?: "null")
                .extra(Extra.MESSAGE_ID, messageId)
                .extra(Extra.BACKGROUND, background.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Show In-App Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.IN_APP_MESSAGE,
            extras = [
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "campaign name")
            ])
    fun showInAppMessage(campaignName: String?) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.IN_APP_MESSAGE)
                .extra(Extra.MESSAGE_ID, campaignName ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Click In-App Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.IN_APP_MESSAGE,
            extras = [
                TelemetryExtra(name = Extra.MESSAGE_ID, value = "campaign name")
            ])
    fun clickInAppMessage(campaignName: String?, buttonText: String?, link: String?) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.IN_APP_MESSAGE)
                .extra(Extra.MESSAGE_ID, campaignName ?: "null")
                .extra(Extra.BUTTON_TEXT, buttonText ?: "null")
                .extra(Extra.LINK, link ?: "null")
                .queue()
    }

    @TelemetryDoc(
            name = "Click Content Hub",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CONTENT_HUB,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}")
            ])
    fun clickContentHub(item: ContentHub.Item) {
        val vertical = when (item) {
            is ContentHub.Item.Shopping -> Extra_Value.SHOPPING
            is ContentHub.Item.Games -> ""
            is ContentHub.Item.Travel -> Extra_Value.TRAVEL
            is ContentHub.Item.News -> Extra_Value.LIFESTYLE
        }
        EventBuilder(Category.ACTION, Method.CLICK, Object.CONTENT_HUB)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
            name = "Reload Content Home",
            category = Category.ACTION,
            method = Method.RELOAD,
            `object` = Object.CONTENT_HOME,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category")
            ])
    fun reloadContentHome(vertical: String, category: String) {
        EventBuilder(Category.ACTION, Method.RELOAD, Object.CONTENT_HOME)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.CATEGORY, category)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Content Home Search Bar",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SEARCH_BAR,
            value = Value.CONTENT_HOME,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}")
            ])
    @JvmStatic
    fun showContentHomeSearchBar(vertical: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.CONTENT_HOME)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
            name = "Select Query Content Home",
            category = Category.ACTION,
            method = Method.TYPE_SELECT_QUERY,
            `object` = Object.SEARCH_BAR,
            value = Value.CONTENT_HOME,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.SOURCE, value = "${Extra_Value.GOOGLE},${Extra_Value.BOOKING_COM}")
            ])
    @JvmStatic
    fun selectQueryContentHome(vertical: String, source: String) {
        EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR, Value.CONTENT_HOME)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.SOURCE, source)
                .queue()
    }

    @TelemetryDoc(
            name = "Open Category",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.CATEGORY,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category")
            ])
    fun openCategory(vertical: String, category: String) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.CATEGORY)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.CATEGORY, category)
                .queue()
    }

    @TelemetryDoc(
            name = "Open Detail Page More",
            category = Category.ACTION,
            method = Method.OPEN,
            `object` = Object.DETAIL_PAGE,
            value = Value.MORE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.ITEM_ID, value = "item id"),
                TelemetryExtra(name = Extra.ITEM_NAME, value = "item name"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "sub category id")
            ])
    fun openDetailPageMore(vertical: String, category: String, itemId: String, itemName: String, subCategoryId: String) {
        EventBuilder(Category.ACTION, Method.OPEN, Object.DETAIL_PAGE, Value.MORE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.ITEM_ID, itemId)
                .extra(Extra.ITEM_NAME, itemName)
                .extra(Extra.SUB_CATEGORY_ID, subCategoryId)
                .queue()
    }

    @TelemetryDoc(
            name = "Start Content Tab",
            category = Category.ACTION,
            method = Method.START,
            `object` = Object.CONTENT_TAB,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id")
            ])
    fun startContentTab(contentTabTelemetryData: ContentTabTelemetryData) {
        EventBuilder(Category.ACTION, Method.START, Object.CONTENT_TAB)
                .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                .extra(Extra.FEED, contentTabTelemetryData.feed)
                .extra(Extra.SOURCE, contentTabTelemetryData.source)
                .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "End Content Tab",
            category = Category.ACTION,
            method = Method.END,
            `object` = Object.CONTENT_TAB,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version"),
                TelemetryExtra(name = Extra.SESSION_TIME, value = "time duration from entering component"),
                TelemetryExtra(name = Extra.URL_COUNTS, value = "url counts duration from entering component"),
                TelemetryExtra(name = Extra.APP_LINK, value = "${Extra_Value.OPEN}|${Extra_Value.INSTALL}|null"),
                TelemetryExtra(name = Extra.SHOW_KEYBOARD, value = "true|false")
            ])
    fun endContentTab(contentTabTelemetryData: ContentTabTelemetryData) {
        EventBuilder(Category.ACTION, Method.END, Object.CONTENT_TAB)
                .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                .extra(Extra.FEED, contentTabTelemetryData.feed)
                .extra(Extra.SOURCE, contentTabTelemetryData.source)
                .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                .extra(Extra.SESSION_TIME, contentTabTelemetryData.sessionTime.toString())
                .extra(Extra.URL_COUNTS, contentTabTelemetryData.urlCounts.toString())
                .extra(Extra.APP_LINK, contentTabTelemetryData.appLink)
                .extra(Extra.SHOW_KEYBOARD, contentTabTelemetryData.showKeyboard.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Category Impression",
            category = Category.ACTION,
            method = Method.IMPRESSION,
            `object` = Object.CATEGORY,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.IMPRESSION, value = "{subcategory id1: max index, subcategory id2: max index...}")
            ])
    fun categoryImpression(version: String, category: String, impression: String) {
        EventBuilder(Category.ACTION, Method.IMPRESSION, Object.CATEGORY)
                .extra(Extra.VERSION_ID, version)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.IMPRESSION, impression)
                .queue()
    }

    @TelemetryDoc(
            name = "Start Vertical Process",
            category = Category.ACTION,
            method = Method.START,
            `object` = Object.PROCESS,
            value = Value.VERTICAL,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}")
            ])
    fun startVerticalProcess(vertical: String) {
        EventBuilder(Category.ACTION, Method.START, Object.PROCESS, Value.VERTICAL)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
            name = "End Vertical Process",
            category = Category.ACTION,
            method = Method.END,
            `object` = Object.PROCESS,
            value = Value.VERTICAL,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}"),
                TelemetryExtra(name = Extra.LOADTIME, value = "[0-9]+")
            ])
    fun endVerticalProcess(vertical: String, loadTime: Long) {
        EventBuilder(Category.ACTION, Method.END, Object.PROCESS, Value.VERTICAL)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.LOADTIME, loadTime.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Show Vertical Onboarding",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.ONBOARDING,
            value = Value.VERTICAL,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}")
            ])
    fun showVerticalOnboarding(vertical: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.ONBOARDING, Value.VERTICAL)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
            name = "Start Tab Swipe Process",
            category = Category.ACTION,
            method = Method.START,
            `object` = Object.PROCESS,
            value = Value.TAB_SWIPE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}")
            ])
    fun startTabSwipeProcess(vertical: String) {
        EventBuilder(Category.ACTION, Method.START, Object.PROCESS, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
            name = "End Tab Swipe Process",
            category = Category.ACTION,
            method = Method.END,
            `object` = Object.PROCESS,
            value = Value.TAB_SWIPE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}"),
                TelemetryExtra(name = Extra.LOADTIME, value = "[0-9]+")
            ])
    fun endTabSwipeProcess(vertical: String, loadTime: Long) {
        EventBuilder(Category.ACTION, Method.END, Object.PROCESS, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.LOADTIME, loadTime.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Start Tab Swipe",
            category = Category.ACTION,
            method = Method.START,
            `object` = Object.TAB_SWIPE,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source")
            ])
    fun startTabSwipe(telemetryData: TabSwipeTelemetryData) {
        EventBuilder(Category.ACTION, Method.START, Object.TAB_SWIPE)
                .extra(Extra.VERTICAL, telemetryData.vertical)
                .extra(Extra.FEED, telemetryData.feed)
                .extra(Extra.SOURCE, telemetryData.source)
                .queue()
    }

    @TelemetryDoc(
            name = "End Tab Swipe",
            category = Category.ACTION,
            method = Method.END,
            `object` = Object.TAB_SWIPE,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.SESSION_TIME, value = "time duration from entering component"),
                TelemetryExtra(name = Extra.URL_COUNTS, value = "url counts duration from entering component"),
                TelemetryExtra(name = Extra.APP_LINK, value = "${Extra_Value.OPEN}|${Extra_Value.INSTALL}|null"),
                TelemetryExtra(name = Extra.SHOW_KEYBOARD, value = "true|false")
            ])
    fun endTabSwipe(telemetryData: TabSwipeTelemetryData) {
        EventBuilder(Category.ACTION, Method.END, Object.TAB_SWIPE)
                .extra(Extra.VERTICAL, telemetryData.vertical)
                .extra(Extra.FEED, telemetryData.feed)
                .extra(Extra.SOURCE, telemetryData.source)
                .extra(Extra.SESSION_TIME, telemetryData.sessionTime.toString())
                .extra(Extra.URL_COUNTS, telemetryData.urlCounts.toString())
                .extra(Extra.APP_LINK, telemetryData.appLink)
                .extra(Extra.SHOW_KEYBOARD, telemetryData.showKeyboard.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Share",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.SHARE,
            extras = [
                TelemetryExtra(name = Extra.MODE, value = "webview"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-9]"),
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id")
            ])
    fun clickContentTabToolbarShare(position: Int, contentTabTelemetryData: ContentTabTelemetryData) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.SHARE)
                .extra(Extra.MODE, Extra_Value.WEBVIEW)
                .extra(Extra.POSITION, position.toString())
                .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                .extra(Extra.FEED, contentTabTelemetryData.feed)
                .extra(Extra.SOURCE, contentTabTelemetryData.source)
                .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Open in browser",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.OPEN_IN_BROWSER,
            extras = [
                TelemetryExtra(name = Extra.MODE, value = "webview"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-9]"),
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id")
            ])
    fun clickContentTabToolbarOpenInBrowser(position: Int, contentTabTelemetryData: ContentTabTelemetryData) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.OPEN_IN_BROWSER)
                .extra(Extra.MODE, Extra_Value.WEBVIEW)
                .extra(Extra.POSITION, position.toString())
                .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                .extra(Extra.FEED, contentTabTelemetryData.feed)
                .extra(Extra.SOURCE, contentTabTelemetryData.source)
                .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Click Toolbar - Reload",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.RELOAD,
            extras = [
                TelemetryExtra(name = Extra.MODE, value = "webview"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-9]"),
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id")
            ])
    fun clickContentTabToolbarReload(position: Int, contentTabTelemetryData: ContentTabTelemetryData) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.RELOAD)
                .extra(Extra.MODE, Extra_Value.WEBVIEW)
                .extra(Extra.POSITION, position.toString())
                .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                .extra(Extra.FEED, contentTabTelemetryData.feed)
                .extra(Extra.SOURCE, contentTabTelemetryData.source)
                .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                .queue()
    }

    fun clickToolbarBack(position: Int) {
        clickContentTabToolbarBack(position, false, null)
    }

    @TelemetryDoc(
            name = "Click Toolbar - Back",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.TOOLBAR,
            value = Value.BACK,
            extras = [
                TelemetryExtra(name = Extra.MODE, value = "webview"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-9]"),
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}"),
                TelemetryExtra(name = Extra.FEED, value = "feed"),
                TelemetryExtra(name = Extra.SOURCE, value = "source"),
                TelemetryExtra(name = Extra.CATEGORY, value = "category"),
                TelemetryExtra(name = Extra.COMPONENT_ID, value = "component id"),
                TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "subcategory id"),
                TelemetryExtra(name = Extra.VERSION_ID, value = "version id")
            ])
    fun clickContentTabToolbarBack(position: Int, isFromVertical: Boolean, contentTabTelemetryData: ContentTabTelemetryData?) {
        if (isFromVertical) {
            requireNotNull(contentTabTelemetryData)
            EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.BACK)
                    .extra(Extra.MODE, Extra_Value.WEBVIEW)
                    .extra(Extra.POSITION, position.toString())
                    .extra(Extra.VERTICAL, contentTabTelemetryData.vertical)
                    .extra(Extra.FEED, contentTabTelemetryData.feed)
                    .extra(Extra.SOURCE, contentTabTelemetryData.source)
                    .extra(Extra.CATEGORY, contentTabTelemetryData.category)
                    .extra(Extra.COMPONENT_ID, contentTabTelemetryData.componentId)
                    .extra(Extra.SUB_CATEGORY_ID, contentTabTelemetryData.subCategoryId)
                    .extra(Extra.VERSION_ID, contentTabTelemetryData.versionId.toString())
                    .queue()
        } else {
            EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.BACK)
                    .extra(Extra.POSITION, position.toString())
                    .extra(Extra.VERTICAL, Extra_Value.ALL)
                    .queue()
        }
    }

    @TelemetryDoc(
        name = "Click Toolbar - Tab Swipe",
        category = Category.ACTION,
        method = Method.CLICK,
        `object` = Object.TOOLBAR,
        value = Value.TAB_SWIPE,
        extras = [TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}"),
            TelemetryExtra(name = Extra.FROM, value = "${Extra_Value.HOME},${Extra_Value.TAB_SWIPE}")
        ])
    @JvmStatic
    fun clickToolbarTabSwipe(vertical: String, from: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.TOOLBAR, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.FROM, from)
                .queue()
    }

    @TelemetryDoc(
        name = "Show Tab Swipe Drawer",
        category = Category.ACTION,
        method = Method.SHOW,
        `object` = Object.DRAWER,
        value = Value.TAB_SWIPE,
        extras = [TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}"),
            TelemetryExtra(name = Extra.FEED, value = "feed")
        ])
    @JvmStatic
    fun showTabSwipeDrawer(vertical: String, feed: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.DRAWER, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.FEED, feed)
                .queue()
    }

    @TelemetryDoc(
        name = "Click Tab Swipe Drawer",
        category = Category.ACTION,
        method = Method.CLICK,
        `object` = Object.DRAWER,
        value = Value.TAB_SWIPE,
        extras = [TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}"),
            TelemetryExtra(name = Extra.FEED, value = "feed")
        ])
    @JvmStatic
    fun clickTabSwipeDrawer(vertical: String, feed: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.DRAWER, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.FEED, feed)
                .queue()
    }

    @TelemetryDoc(
        name = "Add Tab Swipe Tab",
        category = Category.ACTION,
        method = Method.ADD,
        `object` = Object.TAB,
        value = Value.TAB_SWIPE,
        extras = [TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}")])
    @JvmStatic
    fun addTabSwipeTab(vertical: String) {
        EventBuilder(Category.ACTION, Method.ADD, Object.TAB, Value.TAB_SWIPE)
                .extra(Extra.VERTICAL, vertical)
                .queue()
    }

    @TelemetryDoc(
        name = "Change Tab Swipe Settings",
        category = Category.ACTION,
        method = Method.CHANGE,
        `object` = Object.SETTING,
        value = Value.TAB_SWIPE,
        extras = [TelemetryExtra(name = Extra.FEED, value = "feed")])
    @JvmStatic
    fun changeTabSwipeSettings(feed: String) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, Value.TAB_SWIPE)
                .extra(Extra.FEED, feed)
                .queue()
    }

    @TelemetryDoc(
        name = "Show SearchBar from Tab Swipe",
        category = Category.ACTION,
        method = Method.SHOW,
        `object` = Object.SEARCH_BAR,
        value = Value.TAB_SWIPE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}")
        ])
    @JvmStatic
    fun showSearchBarFromTabSwipe(vertical: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SEARCH_BAR, Value.TAB_SWIPE)
            .extra(Extra.VERTICAL, vertical)
            .queue()
    }

    @TelemetryDoc(
        name = "Show Keyboard from Tab Swipe SearchBar",
        category = Category.ACTION,
        method = Method.SHOW_KEYBOARD,
        `object` = Object.SEARCH_BAR,
        value = Value.TAB_SWIPE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}")
        ])
    @JvmStatic
    fun showKeyboardFromTabSwipeSearchBar(vertical: String) {
        EventBuilder(Category.ACTION, Method.SHOW_KEYBOARD, Object.SEARCH_BAR, Value.TAB_SWIPE)
            .extra(Extra.VERTICAL, vertical)
            .queue()
    }

    @TelemetryDoc(
        name = "Start Typing from Tab Swipe SearchBar",
        category = Category.ACTION,
        method = Method.START_TYPING,
        `object` = Object.SEARCH_BAR,
        value = Value.TAB_SWIPE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}")
        ])
    @JvmStatic
    fun startTypingFromTabSwipeSearchBar(vertical: String) {
        EventBuilder(Category.ACTION, Method.START_TYPING, Object.SEARCH_BAR, Value.TAB_SWIPE)
            .extra(Extra.VERTICAL, vertical)
            .queue()
    }

    @TelemetryDoc(
        name = "Search with text in SearchBar",
        category = Category.ACTION,
        method = Method.TYPE_QUERY,
        `object` = Object.SEARCH_BAR,
        value = Value.TAB_SWIPE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}")
        ])
    @JvmStatic
    fun searchWithTextInSearchBar(vertical: String) {
        EventBuilder(Category.ACTION, Method.TYPE_QUERY, Object.SEARCH_BAR, Value.TAB_SWIPE)
            .extra(Extra.VERTICAL, vertical)
            .queue()
    }

    @TelemetryDoc(
        name = "Use SearchSuggestion in Tab Swipe SearchBar",
        category = Category.ACTION,
        method = Method.TYPE_SELECT_QUERY,
        `object` = Object.SEARCH_BAR,
        value = Value.TAB_SWIPE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.REWARDS}"),
            TelemetryExtra(name = Extra.DEFAULT, value = "true,false"),
            TelemetryExtra(name = Extra.KEYWORD, value = "default name xxx,null")
        ])
    @JvmStatic
    fun useSearchSuggestionInTabSwipeSearchBar(vertical: String, isDefault: Boolean, keyword: String) {
        EventBuilder(Category.ACTION, Method.TYPE_SELECT_QUERY, Object.SEARCH_BAR, Value.TAB_SWIPE)
            .extra(Extra.VERTICAL, vertical)
            .extra(Extra.DEFAULT, isDefault.toString())
            .extra(Extra.KEYWORD, keyword)
            .queue()
    }

    @TelemetryDoc(
        name = "Change Travel Settings",
        category = Category.ACTION,
        method = Method.CHANGE,
        `object` = Object.SETTING,
        value = Value.DETAIL_PAGE,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE}"),
            TelemetryExtra(name = Extra.CATEGORY, value = "category"),
            TelemetryExtra(name = Extra.ITEM_ID, value = "item id"),
            TelemetryExtra(name = Extra.ITEM_NAME, value = "item name"),
            TelemetryExtra(name = Extra.SUB_CATEGORY_ID, value = "sub category id"),
            TelemetryExtra(name = Extra.VERSION_ID, value = "version id"),
            TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.SAVE}|${Extra_Value.REMOVE}")
        ])
    fun changeTravelSettings(vertical: String, category: String, itemId: String, itemName: String, subCategoryId: String, versionId: String, action: String) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, Value.DETAIL_PAGE)
            .extra(Extra.VERTICAL, vertical)
            .extra(Extra.CATEGORY, category)
            .extra(Extra.ITEM_ID, itemId)
            .extra(Extra.ITEM_NAME, itemName)
            .extra(Extra.SUB_CATEGORY_ID, subCategoryId)
            .extra(Extra.VERSION_ID, versionId)
            .extra(Extra.ACTION, action)
            .queue()
    }

    @TelemetryDoc(
            name = "Change Personalization in Onboarding",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.ONBOARDING,
            value = Value.PERSONALIZATION,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}"),
                TelemetryExtra(name = Extra.PERSONALIZATION, value = "true,false")
            ])
    @JvmStatic
    fun changePersonalizationInOnboarding(vertical: String, enablePersonalization: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.ONBOARDING, Value.PERSONALIZATION)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.PERSONALIZATION, enablePersonalization.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Change Language in Onboarding",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.ONBOARDING,
            value = Value.LANGUAGE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING},${Extra_Value.TRAVEL},${Extra_Value.LIFESTYLE},${Extra_Value.ALL}"),
                TelemetryExtra(name = Extra.LANGUAGE, value = "language")
            ])
    @JvmStatic
    fun changeLanguageInOnboarding(vertical: String, language: String) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.ONBOARDING, Value.LANGUAGE)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.LANGUAGE, language)
                .queue()
    }

    @TelemetryDoc(
            name = "Click News Settings",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SETTING,
            value = Value.LIFESTYLE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = Extra_Value.LIFESTYLE)
            ])
    @JvmStatic
    fun clickNewsSettings() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SETTING, Value.LIFESTYLE)
                .extra(Extra.VERTICAL, Extra_Value.LIFESTYLE)
                .queue()
    }

    @TelemetryDoc(
        name = "Change Category in Settings",
        category = Category.ACTION,
        method = Method.CHANGE,
        `object` = Object.SETTING,
        value = Value.CATEGORY,
        extras = [
            TelemetryExtra(name = Extra.VERTICAL, value = Extra_Value.LIFESTYLE),
            TelemetryExtra(name = Extra.CATEGORY, value = "category"),
            TelemetryExtra(name = Extra.TO, value = "true,false"),
            TelemetryExtra(name = Extra.PERSONALIZATION, value = "true,false")
        ])
    @JvmStatic
    fun changeCategoryInSettings(category: String, to: Boolean, enablePersonalization: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, Value.CATEGORY)
            .extra(Extra.VERTICAL, Extra_Value.LIFESTYLE)
            .extra(Extra.CATEGORY, category)
            .extra(Extra.TO, to.toString())
            .extra(Extra.PERSONALIZATION, enablePersonalization.toString())
            .queue()
    }

    @TelemetryDoc(
            name = "Change News Settings",
            category = Category.ACTION,
            method = Method.CHANGE,
            `object` = Object.SETTING,
            value = Value.LIFESTYLE,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = Extra_Value.LIFESTYLE),
                TelemetryExtra(name = Extra.LANGUAGE, value = "language"),
                TelemetryExtra(name = Extra.PERSONALIZATION, value = "true,false")
            ])
    @JvmStatic
    fun changeNewsSettings(language: String, enablePersonalization: Boolean) {
        EventBuilder(Category.ACTION, Method.CHANGE, Object.SETTING, Value.LIFESTYLE)
                .extra(Extra.VERTICAL, Extra_Value.LIFESTYLE)
                .extra(Extra.LANGUAGE, language)
                .extra(Extra.PERSONALIZATION, enablePersonalization.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Pin Topsite",
            category = Category.ACTION,
            method = Method.PIN,
            `object` = Object.HOME,
            value = Value.LINK,
            extras = [
                TelemetryExtra(name = Extra.SOURCE, value = "buka|toko...|null"),
                TelemetryExtra(name = Extra.POSITION, value = "[0-9]"),
                TelemetryExtra(name = Extra.DEFAULT, value = "true,false"),
                TelemetryExtra(name = Extra.PINNED, value = "false")
            ])
    fun pinTopSite(source: String?, position: Int, isDefault: Boolean) {
        EventBuilder(Category.ACTION, Method.PIN, Object.HOME, Value.LINK)
                .extra(Extra.SOURCE, source ?: "null")
                .extra(Extra.POSITION, position.toString())
                .extra(Extra.DEFAULT, isDefault.toString())
                .extra(Extra.PINNED, false.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Click Challenge Page Join",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CHALLENGE_PAGE,
            value = Value.JOIN,
            extras = [])
    fun clickChallengePageJoin() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CHALLENGE_PAGE, Value.JOIN)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Task Contextual Hint",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.CONTEXTUAL_HINT,
            value = Value.TASK,
            extras = [])
    fun showTaskContextualHint() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.CONTEXTUAL_HINT, Value.TASK)
                .queue()
    }

    @TelemetryDoc(
            name = "End Task",
            category = Category.ACTION,
            method = Method.END,
            `object` = Object.TASK,
            value = "",
            extras = [
                TelemetryExtra(name = Extra.TASK, value = "[0-9]+"),
                TelemetryExtra(name = Extra.FINISHED, value = "false|true")
            ])
    fun endMissionTask(day: Int, finished: Boolean) {
        EventBuilder(Category.ACTION, Method.END, Object.TASK, null)
                .extra(Extra.TASK, day.toString())
                .extra(Extra.FINISHED, finished.toString())
                .queue()
    }

    @TelemetryDoc(
            name = "Show Challenge Complete Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.CHALLENGE_COMPLETE,
            extras = [])
    fun showChallengeCompleteMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.CHALLENGE_COMPLETE)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Challenge Complete Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.CHALLENGE_COMPLETE,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.DISMISS}|${Extra_Value.LATER}|${Extra_Value.LOGIN}|${Extra_Value.CLOSE}")
            ])
    fun clickChallengeCompleteMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.CHALLENGE_COMPLETE)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Account Sign In",
            category = Category.ACTION,
            method = Method.SIGN_IN,
            `object` = Object.ACCOUNT,
            value = "",
            extras = [])
    fun accountSignIn() {
        EventBuilder(Category.ACTION, Method.SIGN_IN, Object.ACCOUNT, null)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Redeem Page",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.REDEEM_PAGE,
            value = "",
            extras = [])
    fun showCouponPage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.REDEEM_PAGE, null)
                .queue()
    }

    @TelemetryDoc(
            name = "Copy Code on Redeem Page",
            category = Category.ACTION,
            method = Method.COPY,
            `object` = Object.REDEEM_PAGE,
            value = Value.CODE,
            extras = [])
    fun copyCodeOnCouponPage() {
        EventBuilder(Category.ACTION, Method.COPY, Object.REDEEM_PAGE, Value.CODE)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Redeem on Redeem Page",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.REDEEM_PAGE,
            value = Value.USE,
            extras = [])
    fun clickGoUseOnCouponPage() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.REDEEM_PAGE, Value.USE)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Reward Profile",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.PROFILE,
            value = Value.REWARD,
            extras = [])
    fun clickRewardButton() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.PROFILE, Value.REWARD)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Content Home Item",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CONTENT_HOME,
            value = Value.ITEM,
            extras = [
                TelemetryExtra(name = Extra.VERTICAL, value = "${Extra_Value.SHOPPING}|${Extra_Value.TRAVEL}|${Extra_Value.LIFESTYLE}|${Extra_Value.REWARDS}"),
                TelemetryExtra(name = Extra.CATEGORY, value = "${Extra_Value.MISSION}|${Extra_Value.GIFT}|${Extra_Value.EXPLORE}|${Extra_Value.BUCKET_LIST}"),
                TelemetryExtra(name = Extra.ITEM_ID, value = "item id"),
                TelemetryExtra(name = Extra.ITEM_NAME, value = "item name")
            ])
    fun clickContentHomeItem(vertical: String, category: String, itemId: String, itemName: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CONTENT_HOME, Value.ITEM)
                .extra(Extra.VERTICAL, vertical)
                .extra(Extra.CATEGORY, category)
                .extra(Extra.ITEM_ID, itemId)
                .extra(Extra.ITEM_NAME, itemName)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Chellenge Page Login",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.CHALLENGE_PAGE,
            value = Value.LOGIN,
            extras = [])
    fun clickChellengePageLogin() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.CHALLENGE_PAGE, Value.LOGIN)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Update Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.UPDATE,
            extras = [
                TelemetryExtra(name = Extra.CHALLENGE_NAME, value = "from backend MissionName")
            ])
    fun showUpdateMessage(missionName: String) {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.UPDATE)
                .extra(Extra.CHALLENGE_NAME, missionName)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Update Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.UPDATE,
            extras = [
                TelemetryExtra(name = Extra.CHALLENGE_NAME, value = "from backend MissionName"),
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.DISMISS}|${Extra_Value.LATER}|${Extra_Value.UPDATE}|${Extra_Value.CLOSE}")
            ])
    fun clickUpdateMessage(missionName: String, action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.UPDATE)
                .extra(Extra.CHALLENGE_NAME, missionName)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Travel Search Result Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.TRAVEL_SEARCH_RESULT,
            extras = [])
    fun showTravelSearchResultMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.TRAVEL_SEARCH_RESULT)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Travel Search Result Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.TRAVEL_SEARCH_RESULT,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.DISMISS},${Extra_Value.TRAVEL_DISCOVERY},${Extra_Value.GOOGLE_SEARCH}")
            ])
    fun clickTravelSearchResultMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.TRAVEL_SEARCH_RESULT)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Set-default Travel Search Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_TRAVEL_SEARCH,
            extras = [])
    fun showSetDefaultTravelSearchMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.SET_DEFAULT_TRAVEL_SEARCH)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Set-default Travel Search Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_TRAVEL_SEARCH,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.SET_DEFAULT},${Extra_Value.CLOSE}")
            ])
    fun clickSetDefaultTravelSearchMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.SET_DEFAULT_TRAVEL_SEARCH)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Exit Warning Toast",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.TOAST,
            value = Value.EXIT_WARNING,
            extras = [])
    fun showExitToast() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TOAST, Value.EXIT_WARNING)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Set-Default by Settings Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_BY_SETTINGS,
            extras = [])
    fun showSetDefaultBySettingsMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.SET_DEFAULT_BY_SETTINGS)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Set-Default by Link Message",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_BY_LINK,
            extras = [])
    fun showSetDefaultByLinkMessage() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.MESSAGE, Value.SET_DEFAULT_BY_LINK)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Set-Default by Settings Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_BY_SETTINGS,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.OK},${Extra_Value.CANCEL}")
            ])
    fun clickSetDefaultBySettingsMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.SET_DEFAULT_BY_SETTINGS)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Set-Default by Link Message",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.MESSAGE,
            value = Value.SET_DEFAULT_BY_LINK,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.OK},${Extra_Value.CANCEL}")
            ])
    fun clickSetDefaultByLinkMessage(action: String) {
        EventBuilder(Category.ACTION, Method.CLICK, Object.MESSAGE, Value.SET_DEFAULT_BY_LINK)
                .extra(Extra.ACTION, action)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Set-Default Success Toast",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.TOAST,
            value = Value.SET_DEFAULT_SUCCESS,
            extras = [])
    fun showSetDefaultSuccessToast() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.TOAST, Value.SET_DEFAULT_SUCCESS)
                .queue()
    }

    @TelemetryDoc(
            name = "Show Set-Default Try-again Snackbar",
            category = Category.ACTION,
            method = Method.SHOW,
            `object` = Object.SNACKBAR,
            value = Value.SET_DEFAULT_TRY_AGAIN,
            extras = [])
    fun showSetDefaultTryAgainSnackbar() {
        EventBuilder(Category.ACTION, Method.SHOW, Object.SNACKBAR, Value.SET_DEFAULT_TRY_AGAIN)
                .queue()
    }

    @TelemetryDoc(
            name = "Click Set-Default Try-again Snackbar",
            category = Category.ACTION,
            method = Method.CLICK,
            `object` = Object.SNACKBAR,
            value = Value.SET_DEFAULT_TRY_AGAIN,
            extras = [
                TelemetryExtra(name = Extra.ACTION, value = "${Extra_Value.TRY_AGAIN}")
            ])
    fun clickSetDefaultTryAgainSnackBar() {
        EventBuilder(Category.ACTION, Method.CLICK, Object.SNACKBAR, Value.SET_DEFAULT_TRY_AGAIN)
                .extra(Extra.ACTION, Extra_Value.TRY_AGAIN)
                .queue()
    }

    private fun network(): String {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return if (cm?.isActiveNetworkMetered == true) "mobile" else "wifi"
    }

    @TelemetryDoc(
        name = "Start Download File",
        category = Category.ACTION,
        method = Method.START,
        `object` = Object.DOWNLOAD,
        value = Value.FILE,
        extras = [(TelemetryExtra(name = Extra.DOWNLOAD_ID, value = "1,2,3...")),
            (TelemetryExtra(name = Extra.START_TIME, value = "timestamp")),
            (TelemetryExtra(name = Extra.SUPPORT_RESUME, value = "true,false")),
            (TelemetryExtra(name = Extra.VALID_SSL, value = "true,false")),
            (TelemetryExtra(name = Extra.NETWORK, value = "mobile/wifi"))]
    )
    @JvmStatic
    fun startDownloadFile(
        downloadId: String,
        fileSize: Long,
        isValidSSL: Boolean,
        isSupportRange: Boolean
    ) {
        EventBuilder(Category.ACTION, Method.START, Object.DOWNLOAD, Value.FILE)
            .extra(Extra.DOWNLOAD_ID, downloadId)
            .extra(Extra.FILE_SIZE, (fileSize / 1024).toString())
            .extra(Extra.START_TIME, System.currentTimeMillis().toString())
            .extra(Extra.SUPPORT_RESUME, isSupportRange.toString())
            .extra(Extra.VALID_SSL, isValidSSL.toString())
            .extra(Extra.NETWORK, network())
            .queue()
    }

    @TelemetryDoc(
        name = "End Download File",
        category = Category.ACTION,
        method = Method.END,
        `object` = Object.DOWNLOAD,
        value = Value.FILE,
        extras = [(TelemetryExtra(name = Extra.DOWNLOAD_ID, value = "1,2,3...")),
            (TelemetryExtra(name = Extra.END_TIME, value = "timestamp")),
            (TelemetryExtra(name = Extra.FILE_SIZE, value = "number")),
            (TelemetryExtra(name = Extra.PROGRESS, value = "number")),
            (TelemetryExtra(name = Extra.STATUS, value = "1.3")),
            (TelemetryExtra(name = Extra.REASON, value = "1005,1006")),
            (TelemetryExtra(name = Extra.NETWORK, value = "mobile/wifi"))]
    )
    @JvmStatic
    fun endDownloadFile(
        downloadId: Long,
        fileSize: Long?,
        progress: Double?,
        status: Int,
        reason: Int
    ) {
        EventBuilder(Category.ACTION, Method.END, Object.DOWNLOAD, Value.FILE)
            .extra(Extra.DOWNLOAD_ID, downloadId.toString())
            .extra(Extra.END_TIME, System.currentTimeMillis().toString())
            .extra(Extra.FILE_SIZE, fileSize?.div(1024)?.toString() ?: "null")
            .extra(Extra.PROGRESS, progress?.roundToInt()?.toString() ?: "null")
            .extra(Extra.STATUS, status.toString())
            .extra(Extra.REASON, reason.toString())
            .extra(Extra.NETWORK, network())
            .queue()
    }

    internal class EventBuilder @JvmOverloads constructor(
        category: String,
        method: String,
        `object`: String?,
        value: String? = null
    ) {
        var firebaseEvent: FirebaseEvent

        init {
            lazyInit()
            Log.d(TAG, "EVENT:$category/$method/$`object`/$value")

            firebaseEvent = FirebaseEvent.create(category, method, `object`, value)
        }

        fun extra(key: String, value: String): EventBuilder {
            Log.d(TAG, "EXTRA:$key/$value")
            firebaseEvent.param(key, value)
            return this
        }

        fun queue() {
            firebaseEvent.event(appContext)
        }

        companion object {

            const val TAG = "TelemetryWrapper"

            fun lazyInit() {

                if (FirebaseEvent.isInitialized()) {
                    return
                }
                val context = appContext
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

                // default browser already have telemetry

                // NewFeatureNotice already have telemetry

                FirebaseEvent.setPrefKeyWhitelist(prefKeyWhitelist)
            }
        }
    }
}
