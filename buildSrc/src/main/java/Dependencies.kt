object Versions {
    const val min_sdk = 21
    const val target_sdk = 28
    const val compile_sdk = 28
    const val build_tools = "28.0.3"
    const val version_code = 1
    const val version_name = "2.1.15"
    const val android_gradle_plugin = "3.6.1"
    const val gms_oss_licenses_plugin = "0.10.2"
    const val support = "1.0.0"
    const val appcompat = "1.1.0-rc01"
    const val material = "1.0.0"
    const val cardview = "1.0.0"
    const val recyclerview = "1.1.0"
    const val constraint = "1.1.3"
    const val viewpager2 = "1.0.0"
    const val preference = "1.1.0"
    const val palette = "1.0.0"
    const val arch_core_testing = "2.1.0"
    const val arch_work = "2.3.3"
    const val guava_android = "28.2-android"
    const val lifecycle = "2.2.0"
    const val viewmodel = "2.2.0"
    const val room = "2.2.4"
    const val glide = "4.0.0"
    const val kotlin = "1.3.70"
    const val ktlint = "0.32.0"
    const val ktx = "1.2.0"
    const val gms = "11.8.0"
    const val navigation = "2.2.1"
    const val paging = "2.1.2"
    const val findbugs = "3.0.1"
    const val lottie = "3.4.0"
    const val leakcanary = "2.0-beta-3"
    const val android_components = "0.52.0"
    const val adjust = "4.20.0"
    const val android_installreferrer = "1.1.2"
    const val annotation = "1.1.0"
    const val junit = "4.13"
    const val mockito = "3.3.0"
    const val json = "20190722"
    const val robolectric = "4.3.1"
    const val espresso = "3.2.0"
    const val test_core = "1.2.0"
    const val test_ext = "1.1.1"
    const val test_runner = "1.2.0"
    const val uiautomator = "2.2.0"
    const val mockwebserver = "3.7.0"
    const val firebase_core = "17.2.1"
    const val firebase_config = "19.0.3"
    const val firebase_auth = "19.2.0"
    const val firebase_iam = "19.0.3"
    const val fcm = "20.0.1"
    const val crashlytics = "2.10.1"
    const val google_services_plugin = "3.1.1"
    const val fabric_plugin = "1.25.1"
    const val fastlane_screengrab = "1.2.0"
    const val jraska_falcon = "2.0.1"
    const val dagger = "2.16"
    const val play = "1.7.0"
    const val coroutine = "1.3.4"
    const val coroutines_test = "1.3.4"
}

object SystemEnv {
    val google_app_id: String? = System.getenv("google_app_id")
    val default_web_client_id: String? = System.getenv("default_web_client_id")
    val firebase_database_url: String? = System.getenv("firebase_database_url")
    val gcm_defaultSenderId: String? = System.getenv("gcm_defaultSenderId")
    val google_api_key: String? = System.getenv("google_api_key")
    val google_crash_reporting_api_key: String? = System.getenv("google_crash_reporting_api_key")
    val project_id: String? = System.getenv("project_id")
    val auto_screenshot: String? = System.getenv("auto_screenshot")
}

object Localization {
    val KEPT_LOCALE = arrayOf("in", "hi-rIN", "th", "tl", "su", "jv", "vi", "zh-rTW", "ta", "kn", "ml")
}
