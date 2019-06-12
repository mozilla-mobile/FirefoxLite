package org.mozilla.rocket.widget

import android.graphics.drawable.Drawable

open class CustomViewDialogData {
    var drawable: Drawable? = null
    var title: String? = null
    var description: String? = null

    var positiveText: String? = null
    var negativeText: String? = null

    var showCloseButton = false
}
