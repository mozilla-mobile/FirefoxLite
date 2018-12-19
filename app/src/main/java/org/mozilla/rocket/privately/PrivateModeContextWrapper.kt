package org.mozilla.rocket.privately

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import java.io.File

class PrivateModeContextWrapper(base: Context) : ContextWrapper(base) {

    var isInPrivateProcess = false

    /**
     * Override getCacheDir cause when we create a WebView, it'll asked the application's
     * getCacheDir() method and create WebView specific cache.
     */
    override fun getCacheDir(): File {
        if (isInPrivateProcess) {
            return File(super.getCacheDir().absolutePath + "-" + PrivateMode.PRIVATE_PROCESS_NAME)
        }
        return super.getCacheDir()
    }

    /**
     * Override getCacheDir cause when we create a WebView, it'll asked the application's
     * getDir() method and create WebView specific files.
     */
    override fun getDir(name: String?, mode: Int): File {
        if (name == PrivateMode.WEBVIEW_FOLDER_NAME && isInPrivateProcess) {
            return super.getDir("$name-${PrivateMode.PRIVATE_PROCESS_NAME}", mode)
        }
        return super.getDir(name, mode)
    }

    /**
     *  We use PrivateModeActivity's existence to determine if we are in private mode (process)  or not. We don't use
     *  ActivityManager.getRunningAppProcesses() cause it sometimes return null.
     *
     *  The Application class should also rely on this flag to determine if it want to override getDir() and getCacheDir().
     *
     *  Note: we can be in private mode process but don't have any private session yet. ( e.g. We launched
     *  PrivateModeActivity but haven't create any tab yet)
     *
     */
    fun inject(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                // once PrivateModeActivity exist, this process is for private mode
                if (activity is PrivateModeActivity) {
                    isInPrivateProcess = true
                }
            }

            override fun onActivityPaused(activity: Activity?) {
            }

            override fun onActivityResumed(activity: Activity?) {
            }

            override fun onActivityStarted(activity: Activity?) {
            }

            override fun onActivityDestroyed(activity: Activity?) {
            }

            override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            }

            override fun onActivityStopped(activity: Activity?) {
            }
        })
    }
}
