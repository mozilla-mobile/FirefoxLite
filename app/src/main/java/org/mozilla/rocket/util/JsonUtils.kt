package org.mozilla.rocket.util

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun String.toJsonArray(): JSONArray = JSONArray(this)

@Throws(JSONException::class)
fun String.toJsonObject(): JSONObject = JSONObject(this)