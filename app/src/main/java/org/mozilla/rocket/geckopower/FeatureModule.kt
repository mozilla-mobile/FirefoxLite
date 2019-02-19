package org.mozilla.rocket.geckopower

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest

class FeatureModule {
    companion object {
        private const val targetComponent = "org.mozilla.rocket.geckopower.GeckoActivity"
        private const val module = "geckopower"

        @JvmStatic
        fun install(context: Context) {
            val request = SplitInstallRequest.newBuilder().addModule(module).build()


            SplitInstallManagerFactory.create(context).startInstall(request)
                .addOnSuccessListener {
                    Toast.makeText(context, " install success :$it", Toast.LENGTH_SHORT).show()

                    try {
                        Intent().setClassName(context.packageName, targetComponent)
                            .also(context::startActivity)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, " install fail :$it", Toast.LENGTH_SHORT).show()

                    }
                }.addOnFailureListener {
                    Toast.makeText(context, " install fail :$it", Toast.LENGTH_SHORT).show()

                }
        }
    }
}