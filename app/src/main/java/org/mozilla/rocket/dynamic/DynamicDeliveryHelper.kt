package org.mozilla.rocket.dynamic

import android.content.Context
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener

class DynamicDeliveryHelper {

    companion object {
        private const val MODULE_SYNC = "history"
        @JvmStatic
        fun init(context: Context, success: () -> Int) {

            // Creates an instance of SplitInstallManager.
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            if (splitInstallManager.installedModules.contains(MODULE_SYNC)) {
                //return
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
            splitInstallManager.registerListener { state: SplitInstallSessionState ->
                Log.d("aaaa", "SplitInstallSessionState: $state")
            }

            // Creates a request to install a module.
            val request = SplitInstallRequest
                .newBuilder()
                // You can download multiple on demand modules per
                // request by invoking the following method for each
                // module you want to install.
                .addModule("history")
                .build()

            splitInstallManager
                .startInstall(request)
                .addOnSuccessListener { success() }
                .addOnFailureListener { it -> toast("fail $it") }
        }

        private fun toast(s: String) {
            //error code
            // https@ //developer.android.com/reference/com/google/android/play/core/splitinstall/model/SplitInstallErrorCode
//        Toast.makeText(context, s, Toast.LENGTH_LONG).show()
            Log.e("aaaa", "[$s]")
        }

        @JvmStatic
        fun uninstall(context: Context) {
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            if (splitInstallManager.installedModules.contains(MODULE_SYNC)) {
                splitInstallManager.deferredUninstall(listOf(MODULE_SYNC))
                    .addOnSuccessListener { toast("uninstall successful") }
                    .addOnFailureListener { toast("uninstall fail") }
                    .addOnCompleteListener { toast("uninstall complete") }
            }
        }
    }
}