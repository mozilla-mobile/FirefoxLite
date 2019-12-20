package org.mozilla.rocket.extension

import androidx.collection.ArrayMap
import java.net.URI

private val URI.paramMap: Map<String, String>
    get() = if (query != null) {
        ArrayMap<String, String>(1).apply {
            query.split("&")
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

fun URI.getParam(key: String): String? = paramMap[key]