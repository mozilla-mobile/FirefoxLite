package org.mozilla.rocket.component

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.mozilla.focus.activity.MainActivity
import org.mozilla.rocket.content.ContentPortalViewState

class RocketLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = LaunchIntentDispatcher.dispatch(this, intent)
        ContentPortalViewState.isLastSessionContent = false
        when (action) {
            LaunchIntentDispatcher.Action.HANDLED -> finish()
            LaunchIntentDispatcher.Action.NORMAL -> dispatchNormalIntent()
        }
    }

    /**
     * Launch the browser activity.
     */
    private fun dispatchNormalIntent() {
        val intent = Intent(intent)
        intent.setClass(applicationContext, MainActivity::class.java)
        filterFlags(intent)
        startActivity(intent)
        finish()
    }

    private fun filterFlags(intent: Intent) {
        // Explicitly remove the new task and clear task flags (Our browser activity is a single
        // task activity and we never want to start a second task here). See bug 1280112.
        intent.flags = intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
        intent.flags = intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK.inv()
    }
}