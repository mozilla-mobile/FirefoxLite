package org.mozilla.fileutils;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class SerializedItem implements Serializable {

    private String className;
    private String key;
    private byte[] value;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValue() {
        return (value != null) ? ByteBuffer.wrap(value).array() : null;
    }

    public void setValue(byte[] value) {
        this.value = (value != null) ? ByteBuffer.wrap(value).array() : null;
    }
}
