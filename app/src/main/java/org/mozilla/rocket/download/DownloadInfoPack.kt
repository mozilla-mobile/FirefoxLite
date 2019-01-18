package org.mozilla.rocket.download

import org.mozilla.focus.download.DownloadInfo

class DownloadInfoPack(var list: ArrayList<DownloadInfo>, var notifyType: Int, var index: Long) {
    object Constants {
        const val NOTIFY_DATASET_CHANGED = 1
        const val NOTIFY_ITEM_INSERTED = 2
        const val NOTIFY_ITEM_REMOVED = 3
        const val NOTIFY_ITEM_CHANGED = 4
    }
}
