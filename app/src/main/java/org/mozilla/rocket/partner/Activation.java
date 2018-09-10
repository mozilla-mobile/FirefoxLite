package org.mozilla.rocket.partner;

import android.util.MalformedJsonException;

import org.json.JSONException;
import org.json.JSONObject;

class Activation {

    final String owner;
    final long version;
    final String id;
    final long duration;
    final String activationUrl;
    final String customizationUrl;


    private Activation(JSONObject object) throws JSONException {
        owner = object.getString("owner");
        version = object.getInt("version");
        id = object.getString("id");
        duration = object.getLong("duration");
        activationUrl = object.getString("activationUrl");
        customizationUrl = object.getString("customizationUrl");
    }

    boolean matchKeys(String[] keys) {
        if (keys == null || keys.length != 3) {
            return false;
        }

        boolean match = keys[0] != null && keys[0].equals(owner);
        match &= keys[1] != null && keys[1].equals(String.valueOf(version));
        match &= keys[2] != null && keys[2].equals(id);
        return match;
    }

    static Activation from(JSONObject jsonObject) throws MalformedJsonException {
        try {
            return new Activation(jsonObject);
        } catch (JSONException e) {
            throw new MalformedJsonException("Activation information invalid");
        }
    }
}
