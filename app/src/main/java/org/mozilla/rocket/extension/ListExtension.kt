package org.mozilla.rocket.extension

fun <T> List<T>.swap(a: Int, b: Int): List<T> = this
        .toMutableList()
        .also {
            it[a] = this[b]
            it[b] = this[a]
        }