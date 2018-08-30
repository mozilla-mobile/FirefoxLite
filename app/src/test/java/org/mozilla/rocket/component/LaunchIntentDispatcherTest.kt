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
        val command = Intent()
        command.putExtra(PUSH_COMMAND, LaunchIntentDispatcher.Command.SET_DEFAULT.value)
        test(command, LaunchIntentDispatcher.Action.HANDLED)

        val view = Intent()
        view.putExtra(PUSH_OPEN_URL, "https://mozilla.com")
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