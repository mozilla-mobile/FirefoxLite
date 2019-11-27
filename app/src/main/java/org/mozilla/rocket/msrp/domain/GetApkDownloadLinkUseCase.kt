package org.mozilla.rocket.msrp.domain

import org.mozilla.focus.utils.FirebaseContract

class GetApkDownloadLinkUseCase(private val firebaseContract: FirebaseContract) {

    operator fun invoke(): String = firebaseContract.getRcString(RC_KEY_STR_APK_DOWNLOAD_LINK)

    companion object {
        private const val RC_KEY_STR_APK_DOWNLOAD_LINK = "str_apk_download_link"
    }
}