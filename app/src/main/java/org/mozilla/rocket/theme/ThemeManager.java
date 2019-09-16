package org.mozilla.rocket.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import org.mozilla.focus.R;

import java.util.HashSet;

public class ThemeManager {

    private static final String PREF_KEY_STRING_CURRENT_THEME = "pref_key_string_current_theme";

    public interface Themeable {
        void onThemeChanged();

    }

    public interface ThemeHost {
       ThemeManager getThemeManager();
    }

    public enum ThemeSet {
        Default(R.style.ThemeToyDefault),

        Theme1(R.style.ThemeToy01),
        Theme2(R.style.ThemeToy02),
        Theme3(R.style.ThemeToy03),
        Theme4(R.style.ThemeToy04);

        final int style;

        ThemeSet(int styleId) {
            style = styleId;
        }
    }

    private Context baseContext;
    private ThemeSet currentThemeSet = ThemeSet.Default;
    private HashSet<Themeable> subscribedThemeable = new HashSet<>(3);
    private boolean dirty = true;

    public ThemeManager(Context appContext) {
        baseContext = appContext;
        currentThemeSet = loadCurrentTheme(getSharedPreferences(baseContext));
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences;
    }

    public void resetDefaultTheme() {
        setCurrentTheme(ThemeSet.Default);
        notifyThemeChange();
    }

    private void setCurrentTheme(ThemeSet themeSet) {
        saveCurrentTheme(getSharedPreferences(baseContext), themeSet);
        currentThemeSet = themeSet;
        dirty = true;
    }

    public static String getCurrentThemeName(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        ThemeSet currentTheme = loadCurrentTheme(sharedPreferences);

        return currentTheme.name();
    }

    private static ThemeSet loadCurrentTheme(SharedPreferences sharedPreferences) {
        String currentThemeName = sharedPreferences.getString(PREF_KEY_STRING_CURRENT_THEME, ThemeSet.Default.name());
        ThemeSet currentTheme;
        try {
            currentTheme = ThemeSet.valueOf(currentThemeName);
        } catch (Exception e) {
            currentTheme = ThemeSet.Default;
            saveCurrentTheme(sharedPreferences, currentTheme);
        }
        return currentTheme;
    }

    private static void saveCurrentTheme(SharedPreferences sharedPreferences, ThemeSet themeSet) {
        sharedPreferences.edit().putString(PREF_KEY_STRING_CURRENT_THEME, themeSet.name()).apply();
    }

    private static ThemeSet findNextTheme(ThemeSet currentTheme) {
        final int currnetThemeIndex = currentTheme.ordinal();
        final int length = ThemeSet.values().length;
        final int nextThemeIndex = (currnetThemeIndex + 1) % length;

        final ThemeSet nextTheme = ThemeSet.values()[nextThemeIndex];

        return nextTheme;
    }

    public void applyCurrentTheme(Resources.Theme baseTheme) {
        if (dirty) {
            dirty = false;
            baseTheme.applyStyle(currentThemeSet.style, true);
        }
    }

    public ThemeSet toggleNextTheme() {
        final ThemeSet currentTheme = loadCurrentTheme(getSharedPreferences(baseContext));
        final ThemeSet nextTheme = findNextTheme(currentTheme);

        setCurrentTheme(nextTheme);

        notifyThemeChange();

        return nextTheme;
    }

    private void notifyThemeChange() {
        for (Themeable themeable : subscribedThemeable) {
            themeable.onThemeChanged();
        }
    }

    public void subscribeThemeChange(Themeable themeable) {
        subscribedThemeable.add(themeable);
    }

    public void unsubscribeThemeChange(Themeable themeable) {
        subscribedThemeable.remove(themeable);
    }

}
