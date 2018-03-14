package org.mozilla.focus.utils;

import android.os.Bundle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AndroidBundleSerializer {

    private final static String NULL_CLASS_NAME = "null";

    private final Map<String, DataTypeHandler> dataTypeHandlers = new HashMap<>();

    public interface DataTypeHandler {
        String getName();

        SerializedItem create(Bundle bundle, String key) throws IOException;

        void restore(Bundle bundle, SerializedItem serializedItem) throws IOException;
    }

    public AndroidBundleSerializer() {
        register(new NullHandler());
        register(new ByteArrayHandler());
        register(new StringHandler());
        // TODO: support other type of handlers
    }

    private void register(final DataTypeHandler type) {
        final String name = type.getName();
        dataTypeHandlers.put(name, type);
    }

    public void serializeBundle(ObjectOutputStream outputStream, Bundle bundle) throws IOException {
        if (bundle != null && bundle.size() > 0) {
            List<SerializedItem> serializedItemList = new ArrayList<>();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                Object value = bundle.get(key);
                String className = (value != null) ? value.getClass().getCanonicalName() : NULL_CLASS_NAME;
                final DataTypeHandler dataTypeHandler = dataTypeHandlers.get(className);
                if (dataTypeHandler != null) {
                    serializedItemList.add(dataTypeHandler.create(bundle, key));
                }
            }

            outputStream.writeObject(serializedItemList);
        }
    }

    public Bundle deserializeBundle(ObjectInputStream inputStream) throws IOException {
        List<SerializedItem> serializedItemList = null;
        try {
            serializedItemList = (List<SerializedItem>) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (serializedItemList == null || serializedItemList.size() == 0) {
            return null;
        }

        Bundle bundle = new Bundle();
        for (SerializedItem serializedItem : serializedItemList) {
            if (serializedItem != null) {
                final DataTypeHandler dataTypeHandler = dataTypeHandlers.get(serializedItem.getClassName());
                if (dataTypeHandler != null) {
                    dataTypeHandler.restore(bundle, serializedItem);
                }
            }
        }

        return bundle;
    }

    private static class NullHandler implements DataTypeHandler {

        public NullHandler() {
        }

        @Override
        public String getName() {
            return NULL_CLASS_NAME;
        }

        @Override
        public SerializedItem create(final Bundle bundle, final String key) throws IOException {
            SerializedItem serializedItem = new SerializedItem();
            serializedItem.setClassName(this.getName());
            serializedItem.setKey(key);
            serializedItem.setValue(null);

            return serializedItem;
        }

        @Override
        public void restore(final Bundle bundle, final SerializedItem serializedItem) throws IOException {
            bundle.putByteArray(serializedItem.getKey(), serializedItem.getValue());
        }
    }

    private static class ByteArrayHandler implements DataTypeHandler {

        public ByteArrayHandler() {
        }

        @Override
        public String getName() {
            return byte[].class.getCanonicalName();
        }

        @Override
        public SerializedItem create(final Bundle bundle, final String key) throws IOException {

            byte[] value = bundle.getByteArray(key);

            SerializedItem serializedItem = new SerializedItem();
            serializedItem.setClassName(this.getName());
            serializedItem.setKey(key);
            serializedItem.setValue(value);

            return serializedItem;
        }

        @Override
        public void restore(final Bundle bundle, final SerializedItem serializedItem) throws IOException {
            bundle.putByteArray(serializedItem.getKey(), serializedItem.getValue());
        }
    }

    private static class StringHandler implements DataTypeHandler {

        public StringHandler() {
        }

        @Override
        public String getName() {
            return String.class.getCanonicalName();
        }

        @Override
        public SerializedItem create(final Bundle bundle, final String key) throws IOException {

            byte[] value = bundle.getString(key) != null
                    ? bundle.getString(key).getBytes(Charset.forName("UTF-8"))
                    : null;
            if (value == null) {
                return null;
            }

            SerializedItem serializedItem = new SerializedItem();
            serializedItem.setClassName(this.getName());
            serializedItem.setKey(key);
            serializedItem.setValue(value);

            return serializedItem;
        }

        @Override
        public void restore(final Bundle bundle, final SerializedItem serializedItem) throws IOException {
            bundle.putString(serializedItem.getKey(), new String(serializedItem.getValue(), Charset.forName("UTF-8")));
        }
    }
}
