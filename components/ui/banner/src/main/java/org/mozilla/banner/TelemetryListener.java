package org.mozilla.banner;

public interface TelemetryListener {
    void sendClickItemTelemetry(String id, int itemPosition);

    void sendClickBackgroundTelemetry(String id);
}
