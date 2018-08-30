package org.mozilla.focus.telemetry;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.mozilla.focus.utils.SafeIntent;

/* The order of enum value matters. Please put the more specific matching in front of the broader one */
public enum AppLaunchMethod {

    LAUNCHER() {
        @Override
        boolean match(@NonNull SafeIntent intent) {
            return Intent.ACTION_MAIN.equals(intent.getAction());
        }

        @Override
        public void sendLaunchTelemetry() {
            TelemetryWrapper.launchByAppLauncherEvent();
        }
    },

    HOME_SCREEN_SHORTCUT() {
        @Override
        boolean match(@NonNull SafeIntent intent) {
            return Intent.ACTION_VIEW.equals(intent.getAction())
                    && intent.getBooleanExtra(EXTRA_HOME_SCREEN_SHORTCUT, false);
        }

        @Override
        public void sendLaunchTelemetry() {
            TelemetryWrapper.launchByHomeScreenShortcutEvent();
        }
    },

    TEXT_SELECTION_SEARCH() {
        @Override
        boolean match(@NonNull SafeIntent intent) {
            return Intent.ACTION_VIEW.equals(intent.getAction())
                    && intent.getBooleanExtra(EXTRA_TEXT_SELECTION, false);
        }

        @Override
        public void sendLaunchTelemetry() {
            TelemetryWrapper.launchByTextSelectionSearchEvent();
        }
    },

    EXTERNAL_APP() {
        @Override
        boolean match(@NonNull SafeIntent intent) {
            return Intent.ACTION_VIEW.equals(intent.getAction());
        }

        @Override
        public void sendLaunchTelemetry() {
            TelemetryWrapper.launchByExternalAppEvent();
        }
    },

    UNKNOWN() {
        @Override
        boolean match(@NonNull SafeIntent intent) {
            return true;
        }

        @Override
        public void sendLaunchTelemetry() {
            // Do nothing
        }
    };

    public static final String EXTRA_TEXT_SELECTION = "text_selection";
    public static final String EXTRA_HOME_SCREEN_SHORTCUT = "shortcut";

    abstract boolean match(@NonNull SafeIntent intent);

    public abstract void sendLaunchTelemetry();

    // if push, app in foreground, this will be unknow
    public static AppLaunchMethod parse(SafeIntent intent) {
        if (intent != null) {
            for (AppLaunchMethod method : AppLaunchMethod.values()) {
                if (method.match(intent)) {
                    return method;
                }
            }
        }

        return UNKNOWN;
    }
}
