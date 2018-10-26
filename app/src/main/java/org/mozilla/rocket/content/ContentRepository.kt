package org.mozilla.rocket.content

import android.annotation.SuppressLint
import android.content.Context
import org.mozilla.rocket.bhaskar.Repository
import java.lang.IllegalStateException

class ContentRepository {
    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: Repository? = null

        @JvmStatic
        fun getInstance(
            context: Context?
        ): Repository = INSTANCE ?: synchronized(this) {
            if (context == null) {
                throw IllegalStateException("can't create Content Repository with null context")
            }
            INSTANCE ?: buildRepository(context.applicationContext).also { INSTANCE = it }
        }

        private fun buildRepository(context: Context): Repository =
                Repository(context, 521, 10, null, 3, null, null)
    }
}