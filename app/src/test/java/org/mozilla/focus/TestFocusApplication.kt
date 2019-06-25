package org.mozilla.focus

import com.squareup.leakcanary.RefWatcher

class TestFocusApplication : FocusApplication() {

    override fun setupLeakCanary(): RefWatcher {
        return RefWatcher.DISABLED
    }
}
