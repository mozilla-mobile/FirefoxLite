package org.mozilla.rocket.component

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.notification.FirebaseMessagingServiceWrapper.PUSH_COMMAND
import org.mozilla.focus.notification.FirebaseMessagingServiceWrapper.PUSH_OPEN_URL
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LaunchIntentDispatcherTest {

    @Test
    fun dispatch() {
        // unfortunately we can't check this without installing spotify ob a VM with a UI test
        // val applink = Intent()
        // applink.putExtra(PUSH_OPEN_URL, "https://open.spotify.com")
        // test(applink, LaunchIntentDispatcher.Action.HANDLED)

        val command = Intent()
        command.putExtra(PUSH_COMMAND, LaunchIntentDispatcher.Command.SET_DEFAULT.value)
        test(command, LaunchIntentDispatcher.Action.NORMAL)

        val view = Intent()
        view.putExtra(PUSH_OPEN_URL, "https://open.spotify.com")
        test(view, LaunchIntentDispatcher.Action.NORMAL)


        val launch = Intent()
        launch.action = ACTION_MAIN
        test(launch, LaunchIntentDispatcher.Action.NORMAL)
    }

    fun test(intent: Intent, value: LaunchIntentDispatcher.Action) {
        RuntimeEnvironment.application.applicationContext?.apply {
            val dispatch = LaunchIntentDispatcher.dispatch(this, intent)
            assert(dispatch == value)
        }
    }
}