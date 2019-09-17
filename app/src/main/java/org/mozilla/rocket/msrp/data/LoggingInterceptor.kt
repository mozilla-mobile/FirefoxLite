package org.mozilla.rocket.msrp.data

import android.util.Log
import mozilla.components.concept.fetch.Response
import mozilla.components.concept.fetch.interceptor.Interceptor

class LoggingInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        Log.d("FETCH", "Request to ${chain.request.url}")

        val startTime = System.currentTimeMillis()

        val response = chain.proceed(chain.request)

        val took = System.currentTimeMillis() - startTime
        Log.d("FETCH", "[${response.status}] took $took ms")

        return response
    }
}