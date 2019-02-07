package org.mozilla.rocket.component

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.Job
import org.mozilla.focus.activity.MainActivity

class RocketLauncherActivity : AppCompatActivity()
//    , CoroutineScope
{
    private lateinit var job: Job
//    override val coroutineContext: CoroutineContext
//        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        job = Job()

        val action = LaunchIntentDispatcher.dispatch(this, intent)
        when (action) {
            LaunchIntentDispatcher.Action.HANDLED -> finish()
            LaunchIntentDispatcher.Action.NORMAL -> dispatchNormalIntent()
        }
    }

    /**
     * Launch the browser activity.
     */
    private fun dispatchNormalIntent() {
//        this.launch {
            val intent = Intent(intent)
            intent.setClass(applicationContext, MainActivity::class.java)
            filterFlags(intent)
            startActivity(intent)
            finish()
//        }

    }

    private fun filterFlags(intent: Intent) {
        // Explicitly remove the new task and clear task flags (Our browser activity is a single
        // task activity and we never want to start a second task here). See bug 1280112.
        intent.flags = intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
        intent.flags = intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK.inv()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}