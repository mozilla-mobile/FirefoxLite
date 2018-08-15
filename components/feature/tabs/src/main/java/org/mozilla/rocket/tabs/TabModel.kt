package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.os.Bundle

data class TabModel(
    val id: String,
    var parentId: String?,
    var title: String?,
    var url: String?
) {

    /**
     * Favicon bitmap for tab tray item.
     */
    var favicon: Bitmap? = null

    /**
     * Thumbnail bitmap for tab previewing.
     */
    var thumbnail: Bitmap? = null

    /**
     * ViewState for this Tab. Usually to fill by WebView.saveViewState(Bundle)
     * Set it as @Ignore to avoid storing this field into database.
     * It will be serialized to a file and save the uri path into webViewStateUri field.
     */
    var webViewState: Bundle? = null

    fun isValid(): Boolean {
        return id.isNotBlank() && (url?.isNotBlank() ?: false)
    }
}
