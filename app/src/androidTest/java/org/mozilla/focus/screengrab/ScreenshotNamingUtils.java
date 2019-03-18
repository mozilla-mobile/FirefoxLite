package org.mozilla.focus.screengrab;

public class ScreenshotNamingUtils {

    private static final String ONBOARDING_BASE = "01_onboarding_";
    static final String ONBOARDING_1 = ONBOARDING_BASE + 1;
    static final String ONBOARDING_2 = ONBOARDING_BASE + 2;
    static final String ONBOARDING_3 = ONBOARDING_BASE + 3;
    static final String ONBOARDING_4 = ONBOARDING_BASE + 4;
    static final String ONBOARDING_COLOR_THEME = ONBOARDING_BASE + 5;
    static final String ONBOARDING_MY_SHOT = ONBOARDING_BASE + 6;
    static final String ONBOARDING_NIGHT_MODE = ONBOARDING_BASE + 7;

    private static final String HOME_BASE = "02_home_";

    static final String HOME_REMOVE_TOP_SITE = HOME_BASE + "01";
    static final String HOME_MENU = HOME_BASE + "02";
    static final String HOME_MENU_NO_BOOKMARKS = HOME_BASE + "03";
    static final String HOME_MENU_NO_DOWNLOAD = HOME_BASE + "04";
    static final String HOME_MENU_NO_HISTORY = HOME_BASE + "05";
    static final String HOME_MENU_NO_SCREENSHOT = HOME_BASE + "06";

    static final String HOME_MENU_TURBO_MODE_DISABLED = HOME_BASE + "07";
    static final String HOME_MENU_TURBO_MODE_ENABLED = HOME_BASE + "08";
    static final String HOME_MENU_BLOCK_IMG_ENABLED = HOME_BASE + "09";
    static final String HOME_MENU_BLOCK_IMG_DISABLED = HOME_BASE + "10";
    static final String HOME_MENU_CLEAR_CACHE = HOME_BASE + "11";
    static final String HOME_NOTI_RATE_APP = HOME_BASE + "12";
    static final String HOME_NOTI_DEFAULT_SETTING = HOME_BASE + "13";
    static final String HOME_NOTI_PRIVACY_UPDATE = HOME_BASE + "14";
    static final String HOME_VPN_RECOMMENDER = HOME_BASE + "15";


    private static final String BROWSER_BASE = "03_browser_";
    static final String BROWSER_CONTEXT_MENU = BROWSER_BASE + 1;
    static final String BROWSER_ERROR_PAGE = BROWSER_BASE + 2;
    static final String BROWSER_NO_LOCATION_SNACKBAR = BROWSER_BASE + 3;
    static final String BROWSER_NO_LOCATION_DIALOG = BROWSER_BASE + 4;
    static final String BROWSER_TEXT_ACTION_DAILOG = BROWSER_BASE + 5;
    static final String BROWSER_TAB_TRAY = BROWSER_BASE + 6;
    static final String BROWSER_TAB_TRAY_CLOSE_DIALOG = BROWSER_BASE + 7;
    static final String BROWSER_NEW_TAB_OPENED = BROWSER_BASE + 8;

    private static final String BOOKMARK_BASE = "04_bookmark_";
    static final String BOOKMARK_ADD_SNACKBAR = BOOKMARK_BASE + 1;
    static final String BOOKMARK_REMOVED = BOOKMARK_BASE + 2;
    static final String BOOKMARK_LIST = BOOKMARK_BASE + 3;
    static final String BOOKMARK_LIST_MENU = BOOKMARK_BASE + 4;
    static final String BOOKMARK_EDIT = BOOKMARK_BASE + 5;
    static final String BOOKMARK_UPDATED = BOOKMARK_BASE + 6;

    private static final String DOWNLOAD_BASE = "05_download_";
    static final String DOWNLOAD_NO_PERMISSION = DOWNLOAD_BASE + 1;
    static final String DOWNLOAD_DOWNLOADING = DOWNLOAD_BASE + 2;
    static final String DOWNLOAD_PROTOCOL_NOT_SUPPORT = DOWNLOAD_BASE + 3;
    static final String DOWNLOAD_SD_FULL = DOWNLOAD_BASE + 4;
    static final String DOWNLOAD_DOWNLOADED = DOWNLOAD_BASE + 5;
    static final String DOWNLOAD_LIST_MENU = DOWNLOAD_BASE + 6;
    //TODO
    static final String DOWNLOAD_LIST_MENU_CANCEL = DOWNLOAD_BASE + 7;
    static final String DOWNLOAD_DELETED = DOWNLOAD_BASE + 8;
    static final String DOWNLOAD_CANCELED = DOWNLOAD_BASE + 9;
    static final String DOWNLOAD_CANT_FIND_THE_FILE = DOWNLOAD_BASE + 10;

    private static final String HISTORY_BASE = "06_history_";
    static final String HISTORY_PANEL = HISTORY_BASE + 1;
    static final String HISTORY_DELETE = HISTORY_BASE + 2;
    static final String HISTORY_CLEAR_ALL = HISTORY_BASE + 3;

    private static final String SCREENSHOT_BASE = "07_screenshot_";
    static final String SCREENSHOT_CAPTURING = SCREENSHOT_BASE + 1;
    static final String SCREENSHOT_FAILED_TO_CAPTURE = SCREENSHOT_BASE + 2;
    static final String SCREENSHOT_PANEL_AND_SAVED = SCREENSHOT_BASE + 3;
    static final String SCREENSHOT_INFO = SCREENSHOT_BASE + 4;
    static final String SCREENSHOT_DELETE = SCREENSHOT_BASE + 5;
    static final String SCREENSHOT_DELETED = SCREENSHOT_BASE + 6;

    private static final String SETTINGS_BASE = "08_settings_";
    static final String SETTINGS_LIST_1 = SETTINGS_BASE + "01";
    static final String SETTINGS_LIST_2 = SETTINGS_BASE + "02";
    static final String SETTINGS_LANGUAGE_1 = SETTINGS_BASE + "03";
    static final String SETTINGS_LANGUAGE_2 = SETTINGS_BASE + "04";
    static final String SETTINGS_SEARCH_ENGINE = SETTINGS_BASE + "05";
    static final String SETTINGS_CLEAR_DATA = SETTINGS_BASE + "06";
    static final String SETTINGS_CLEAR_DATA_TOAST = SETTINGS_BASE + "07";
    static final String SETTINGS_SAVE_DOWNLOADS = SETTINGS_BASE + "08";
    static final String SETTINGS_FEEDBACK = SETTINGS_BASE + "09";
    static final String SETTINGS_SHARE = SETTINGS_BASE + "10";
    static final String SETTINGS_SHARE_GMAIL = SETTINGS_BASE + "11";
    static final String SETTINGS_ABOUT = SETTINGS_BASE + "12";
    static final String SETTINGS_ABOUT_YOUR_RIGHT = SETTINGS_BASE + "13";

    private static final String PRIVATE_BROWSING_BASE = "09_private_";
    static final String PRIVATE_ERASING_TOAST = PRIVATE_BROWSING_BASE + "1";
    static final String PRIVATE_ERASING_NOTIFICATION = PRIVATE_BROWSING_BASE + "2";

    private static final String PORTAL_NEWS_BASE = "10_portalNews_";
    static final String PORTAL_NEWS_ONBOARDING = PORTAL_NEWS_BASE + 1;
    static final String PORTAL_NEWS_SOURCE = PORTAL_NEWS_BASE + 2;
    static final String PORTAL_NEWS_SOURCE_DIALOG = PORTAL_NEWS_BASE + 3;
    static final String PORTAL_NEWS_ABOUT_LIFE_FEED = PORTAL_NEWS_BASE + 4;
}
