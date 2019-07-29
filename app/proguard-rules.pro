# We do not want to obfuscate - It's just painful to debug without the right mapping file.
-dontobfuscate

# Adjust
-keep public class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep class dalvik.system.VMRuntime {
    java.lang.String getRuntime();
}
-keep class android.os.Build {
    java.lang.String[] SUPPORTED_ABIS;
    java.lang.String CPU_ABI;
}
-keep class android.content.res.Configuration {
    android.os.LocaledList getLocales();
    java.util.Locale locale;
}
-keep class android.os.LocaledList {
    java.util.Locale get(int);
}

# Customized BottomSheetBehavior for ViewPager
-keep class org.mozilla.rocket.widget.ViewPagerBottomSheetBehavior

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Integrate Glide source code
-dontwarn android.graphics.Bitmap$Config
-dontwarn android.app.FragmentManager

# For Fragments which be created by xml of Android Navigation Architecture Components
-keep public class org.mozilla.rocket.** extends androidx.fragment.app.Fragment

# kotlinx.coroutines
-dontwarn kotlinx.atomicfu.AtomicBoolean