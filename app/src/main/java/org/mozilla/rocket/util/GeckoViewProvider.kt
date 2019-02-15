package org.mozilla.rocket.util

import android.content.Context
import org.mozilla.rocket.tabs.TabView

interface IGeckoViewProvider {
    fun create(ctx: Context): TabView
}