package org.mozilla.rocket.util

import org.json.JSONArray
import org.json.JSONException

@Throws(JSONException::class)
fun String.toJsonArray(): JSONArray = JSONArray(this)