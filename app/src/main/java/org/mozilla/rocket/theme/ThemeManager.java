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
        Context getApplicationContext();

        ThemeManager getThemeManager();
    }

    enum ThemeSet {
        Default(R.style.ThemeToyDefault),

        CatalinaBlue(R.style.ThemeToy01),
        Gossamer(R.style.ThemeToy02),
        BlueViolet(R.style.ThemeToy03),
        CornflowerBlue(R.style.ThemeToy04);

        final int style;

        ThemeSet(int styleId) {
            style = styleId;
        }
    }

    private Context baseContext;
    private ThemeSet currentThemeSet = ThemeSet.Default;
    private HashSet<Themeable> subscribedThemeable = new HashSet<>(3);
    private boolean dirty = true;

    public ThemeManager(ThemeHost themeHost) {
        baseContext = themeHost.getApplicationContext();
        currentThemeSet = loadCurrentTheme(getSharedPreferences());
    }

    private SharedPreferences getSharedPreferences() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext);
        return sharedPreferences;
    }

    public void resetDefaultTheme() {
        setCurrentTheme(ThemeSet.Default);
        notifyThemeChange();
    }

    private void setCurrentTheme(ThemeSet themeSet) {
        saveCurrentTheme(getSharedPreferences(), themeSet);
        currentThemeSet = themeSet;
        dirty = true;
    }

    private static ThemeSet loadCurrentTheme(SharedPreferences sharedPreferences) {
        String currentThemeName = sharedPreferences.getString(PREF_KEY_STRING_CURRENT_THEME, ThemeSet.Default.name());
        ThemeSet currentTheme;
        try {
            currentTheme = ThemeSet.valueOf(currentThemeName);
        } catch (Exception e) {
            currentTheme = ThemeSet.Default;
            sharedPreferences.edit().putString(PREF_KEY_STRING_CURRENT_THEME, currentTheme.name()).apply();
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

    public Resources.Theme applyCurrentTheme(Resources.Theme baseTheme) {
        if (dirty) {
            dirty = false;
            baseTheme.applyStyle(currentThemeSet.style, true);
        }
        return baseTheme;
    }

    public void toggleNextTheme() {
        final ThemeSet currentTheme = loadCurrentTheme(getSharedPreferences());
        final ThemeSet nextTheme = findNextTheme(currentTheme);

        setCurrentTheme(nextTheme);

        notifyThemeChange();
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
