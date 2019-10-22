package org.mozilla.focus.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.VisibleForTesting;

public class NewFeatureNotice {

    private static final String PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN = "firstrun_shown";
    private static final String PREF_KEY_INT_FEATURE_UPGRADE_VERSION = "firstrun_upgrade_version";
    private static final String PREF_KEY_BOOLEAN_HOME_PAGE_ONBOARDING = "has_home_page_onboarding_shown";
    private static final String PREF_KEY_BOOLEAN_HOME_SHOPPING_SEARCH_ONBOARDING = "has_home_shopping_search_onboarding_shown";

    private static final int MULTI_TAB_FROM_VERSION_1_0_TO_2_0 = 1;
    private static final int FIREBASE_FROM_VERSION_2_0_TO_2_1 = 2;
    private static final int LITE_FROM_VERSION_2_1_TO_4_0 = 3;
    private static final int LITE_FROM_VERSION_4_0_TO_1_1_4 = 4;
    private static final int LITE_FROM_VERSION_1_0_0_TO_2_0_0 = 5;

    private static NewFeatureNotice instance;

    private final SharedPreferences preferences;

    public synchronized static NewFeatureNotice getInstance(Context context) {
        if (instance == null) {
            instance = new NewFeatureNotice(context.getApplicationContext());
        }
        return instance;
    }

    private NewFeatureNotice(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean shouldShowLiteUpdate() {
        return hasShownFirstRun() && needToShow100To200Update();
    }

    public boolean needToShow100To200Update() {
        return getLastShownFeatureVersion() < LITE_FROM_VERSION_1_0_0_TO_2_0_0;
    }

    public void setLiteUpdateDidShow() {
        setLastShownFeatureVersion(LITE_FROM_VERSION_1_0_0_TO_2_0_0);
    }

    public boolean shouldShowPrivacyPolicyUpdate() {
        if (isNewlyInstalled()) {
            setPrivacyPolicyUpdateNoticeDidShow();
            return false;
        }

        return FIREBASE_FROM_VERSION_2_0_TO_2_1 == getLastShownFeatureVersion() + 1;
    }

    public void setPrivacyPolicyUpdateNoticeDidShow() {
        setLastShownFeatureVersion(FIREBASE_FROM_VERSION_2_0_TO_2_1);
    }

    public boolean hasShownFirstRun() {
        return preferences.getBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, false);
    }

    public void setFirstRunDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, true)
                .apply();
    }

    @VisibleForTesting
    public void resetFirstRunDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_FIRSTRUN_SHOWN, false)
                .putInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, 0)
                .apply();
    }

    private boolean isNewlyInstalled() {
        return !hasShownFirstRun() && (getLastShownFeatureVersion() == 0);
    }

    private void setLastShownFeatureVersion(int featureVersion) {
        if (getLastShownFeatureVersion() >= featureVersion) {
            return;
        }

        preferences.edit()
                .putInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, featureVersion)
                .apply();
    }

    public int getLastShownFeatureVersion() {
        return preferences.getInt(PREF_KEY_INT_FEATURE_UPGRADE_VERSION, 0);
    }

    public boolean hasHomePageOnboardingShown() {
        return preferences.getBoolean(PREF_KEY_BOOLEAN_HOME_PAGE_ONBOARDING, false);
    }

    public void setHomePageOnboardingDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_HOME_PAGE_ONBOARDING, true)
                .apply();
    }

    @VisibleForTesting
    public void resetHomePageOnboardingDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_HOME_PAGE_ONBOARDING, false)
                .apply();
    }

    public void setHomeShoppingSearchOnboardingDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_HOME_SHOPPING_SEARCH_ONBOARDING, true)
                .apply();
    }

    public boolean hasHomeShoppingSearchOnboardingShown() {
        return preferences.getBoolean(PREF_KEY_BOOLEAN_HOME_SHOPPING_SEARCH_ONBOARDING, false);
    }

    @VisibleForTesting
    public void resetHomeShoppingSearchOnboardingDidShow() {
        preferences.edit()
                .putBoolean(PREF_KEY_BOOLEAN_HOME_SHOPPING_SEARCH_ONBOARDING, false)
                .apply();
    }
}
