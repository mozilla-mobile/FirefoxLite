package org.mozilla.focus.history.model;

/**
 * Created by hart on 11/08/2017.
 */

public class DateSection {
    private long timestamp;

    public DateSection(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
