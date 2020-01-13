package org.mozilla.rocket.extension

import androidx.collection.ArrayMap
import java.net.URI
import java.net.URLDecoder

private val URI.paramMap: Map<String, String>
    get() = if (rawQuery != null) {
        ArrayMap<String, String>().apply {
            rawQuery.split("&")
                    .map { it.split("=") }
                    .filter { it.size == 2 }
                    .forEach {
                        val key = it[0]
                        val value = it[1]
                        put(key, value)
                    }
        }
    } else {
        emptyMap()
    }

fun URI.getParam(key: String): String = URLDecoder.decode(paramMap[key] ?: "", "UTF-8")