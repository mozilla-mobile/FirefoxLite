package org.mozilla.rocket.dynamic

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener

class DynamicDeliveryHelper {

    companion object {

        const val TAG = "aabworkshop"

        private const val MODULE_SYNC = "history"


        @JvmStatic
        fun installHistoryModule(context: Context) {

            // Creates an instance of SplitInstallManager.
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            if (splitInstallManager.installedModules.contains(MODULE_SYNC)) {
                toast(context, "$MODULE_SYNC module installed")
            }

            /**
            public @interface SplitInstallSessionStatus {
            int UNKNOWN = 0;
            int PENDING = 1;
            int REQUIRES_USER_CONFIRMATION = 8;
            int DOWNLOADING = 2;
            int DOWNLOADED = 3;
            int INSTALLING = 4;
            int INSTALLED = 5;
            int FAILED = 6;
            int CANCELING = 9;
            int CANCELED = 7;
            }

            public @interface SplitInstallErrorCode {
            int NO_ERROR = 0;
            int ACTIVE_SESSIONS_LIMIT_EXCEEDED = -1;
            int MODULE_UNAVAILABLE = -2;
            int INVALID_REQUEST = -3;
            int SESSION_NOT_FOUND = -4;
            int API_NOT_AVAILABLE = -5;
            int NETWORK_ERROR = -6;
            int ACCESS_DENIED = -7;
            int INCOMPATIBLE_WITH_EXISTING_SESSION = -8;
            int SERVICE_DIED = -9;
            int INTERNAL_ERROR = -100;
            }

             **/
            val listener =
                SplitInstallStateUpdatedListener { state ->
                    toast(context, "SplitInstallSessionState: SplitInstallStateUpdatedListener:$state")
                }
            splitInstallManager.registerListener(listener)

            val request = SplitInstallRequest
                .newBuilder()
                .addModule(MODULE_SYNC)
                .build()

            splitInstallManager
                .startInstall(request)
                .addOnSuccessListener { sessionId ->
                    toast(context, "startInstall success id------$sessionId")
                    // SplitCompat.install(context)
                    startHistoryModule(context)

                }
                .addOnFailureListener { toast(context, "startInstall fail------$it") }

            splitInstallManager.unregisterListener(listener)
        }

        private fun toast(context: Context, s: String) {
            //error code
            // https@ //developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode
            Toast.makeText(context, s, Toast.LENGTH_LONG).show()
            Log.e(TAG, "toast : [$s]")
        }

        @JvmStatic
        fun uninstall(context: Context) {
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            if (splitInstallManager.installedModules.contains(MODULE_SYNC)) {
                splitInstallManager.deferredUninstall(listOf(MODULE_SYNC))
                    .addOnSuccessListener { toast(context, "uninstall successful") }
                    .addOnFailureListener { toast(context, "uninstall fail") }
                    .addOnCompleteListener { toast(context, "uninstall complete") }
            }
        }

        private fun startHistoryModule(context: Context) {
            try {
                val loginActivity =
                    Intent().setClassName(context.packageName, "org.mozilla.rocket.history.LoginActivity")

                context.startActivity(loginActivity)
            } catch (e: Exception) {
                toast(context, "startHistoryModule error" + e.localizedMessage)
            }
        }
    }
}