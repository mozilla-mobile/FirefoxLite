package org.mozilla.rocket.download

import android.app.DownloadManager
import android.arch.lifecycle.LiveData
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import org.mozilla.focus.download.DownloadInfo
import org.mozilla.focus.download.DownloadInfoManager
import org.mozilla.focus.utils.CursorUtils
import org.mozilla.threadutils.ThreadUtils

class DownloadInfoLiveData() : LiveData<DownloadInfoBundle>() {

    private val downloadInfoBundle: DownloadInfoBundle = DownloadInfoBundle(ArrayList(), -1, -1)

}
