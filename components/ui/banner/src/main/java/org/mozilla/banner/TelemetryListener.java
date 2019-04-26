package org.mozilla.banner;

public interface TelemetryListener {
    void sendClickItemTelemetry(String jsonString, int itemPosition);

    void sendClickBackgroundTelemetry(String jsonString);
}
