package org.mozilla.rocket.component

import android.app.Application
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import androidx.test.core.app.ApplicationProvider
import org.mozilla.focus.notification.FirebaseMessagingServiceWrapper.Companion.PUSH_COMMAND
import org.mozilla.focus.notification.FirebaseMessagingServiceWrapper.Companion.PUSH_OPEN_URL

// @RunWith(RobolectricTestRunner::class)
class LaunchIntentDispatcherTest {

//    @Test
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
        ApplicationProvider.getApplicationContext<Application>().apply {
            val dispatch = LaunchIntentDispatcher.dispatch(this, intent)
            assert(dispatch == value)
        }
    }
}