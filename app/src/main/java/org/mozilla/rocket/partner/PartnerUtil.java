package org.mozilla.rocket.partner;

import android.content.ContentResolver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

final class PartnerUtil {

    private static final String LOGGABLE_TAG = "vendor.partner";
    private static final String DEBUG_MOCK_TAG = "vendor.partner.mock";
    private static final String LOG_TAG = PartnerUtil.class.getSimpleName();

    static boolean propNA = false;

    static boolean debugMock() {
        return Log.isLoggable(DEBUG_MOCK_TAG, Log.DEBUG);
    }

    static String getProperty(String key) {
        if (propNA) {
            return null;
        }

        if (debugMock() && key.equals(PartnerActivator.PARTNER_ACTIVATION_KEY)) {
            return "moz/1/DEVCFA";
        }

        try {
            return getPropertiesFromSystem(key);
        } catch (Exception e) {
            try {
                return getPropertiesFromProcess(key);
            } catch (Exception e1) {
                propNA = true;
                return null;
            }
        }
    }

    private static String getPropertiesFromSystem(String key) throws Exception {
        Class c = Class.forName("android.os.SystemProperties");
        Method method = c.getDeclaredMethod("get", String.class);
        return (String) method.invoke(null, key);
    }

    private static String getPropertiesFromProcess(String PropKey) throws Exception {
        Process p;
        String propvalue = "";
        p = new ProcessBuilder("/system/bin/getprop", PropKey).redirectErrorStream(false).start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                propvalue = line;
            }
        }
        p.destroy();
        return propvalue;
    }

    /**
     * Returns the unique identifier for the device
     *
     * @return unique identifier for the device
     */
    static String getDeviceIdentifier(@NonNull ContentResolver contentResolver) {
        final String deviceUniqueIdentifier;
        deviceUniqueIdentifier = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        return deviceUniqueIdentifier;
    }

    static int log(String msg) {
        return log(null, msg);
    }

    static int log(Throwable throwable, String msg) {
        if (!Log.isLoggable(LOGGABLE_TAG, Log.DEBUG)) {
            return 0;
        }
        if (throwable != null) {
            return Log.d(LOG_TAG, msg, throwable);
        } else {
            return Log.d(LOG_TAG, msg);
        }
    }
}
