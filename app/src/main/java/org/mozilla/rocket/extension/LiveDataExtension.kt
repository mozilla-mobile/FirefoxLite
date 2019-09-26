/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.extension

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <T> LiveData<T>.nonNullObserve(owner: LifecycleOwner, observer: (t: T) -> Unit) {
    this.observe(owner, Observer {
        observer(it!!)
    })
}

/** Uses `Transformations.map` on a LiveData */
fun <X, Y> LiveData<X>.map(body: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, body)
}

/** Uses `Transformations.switchMap` on a LiveData */
fun <X, Y> LiveData<X>.switchMap(body: (X) -> LiveData<Y>): LiveData<Y> {
    return Transformations.switchMap(this, body)
}

fun <X, Y> LiveData<X>.switchFrom(source: LiveData<Y>): LiveData<X> =
        source.switchMap { this.map { it } }

fun <A, B> combineLatest(a: LiveData<A>, b: LiveData<B>): LiveData<Pair<A, B>> {
    return MediatorLiveData<Pair<A, B>>().apply {
        var lastA: A? = null
        var lastB: B? = null

        addSource(a) {
            if (it == null && value != null) value = null
            lastA = it
            if (lastA != null && lastB != null) value = lastA!! to lastB!!
        }

        addSource(b) {
            if (it == null && value != null) value = null
            lastB = it
            if (lastA != null && lastB != null) value = lastA!! to lastB!!
        }
    }
}

fun <A, B, C> combineLatest(a: LiveData<A>, b: LiveData<B>, c: LiveData<C>): LiveData<Triple<A?, B?, C?>> {

    fun Triple<A?, B?, C?>?.copyWithFirst(first: A?): Triple<A?, B?, C?> {
        if (this@copyWithFirst == null) return Triple<A?, B?, C?>(first, null, null)
        return this@copyWithFirst.copy(first = first)
    }

    fun Triple<A?, B?, C?>?.copyWithSecond(second: B?): Triple<A?, B?, C?> {
        if (this@copyWithSecond == null) return Triple<A?, B?, C?>(null, second, null)
        return this@copyWithSecond.copy(second = second)
    }

    fun Triple<A?, B?, C?>?.copyWithThird(third: C?): Triple<A?, B?, C?> {
        if (this@copyWithThird == null) return Triple<A?, B?, C?>(null, null, third)
        return this@copyWithThird.copy(third = third)
    }

    return MediatorLiveData<Triple<A?, B?, C?>>().apply {
        addSource(a) { value = value.copyWithFirst(it) }
        addSource(b) { value = value.copyWithSecond(it) }
        addSource(c) { value = value.copyWithThird(it) }
    }
}