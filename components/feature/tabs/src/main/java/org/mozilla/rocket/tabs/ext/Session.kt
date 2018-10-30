package org.mozilla.rocket.tabs.ext

import android.graphics.Bitmap
import android.text.TextUtils
import org.mozilla.rocket.tabs.Session
import java.util.WeakHashMap

// Extension methods on the Session class. This is used for additional session data that is not part
// of the upstream browser-session component yet.
const val ID_EXTERNAL = "_open_from_external_"
const val BLANK_URL = "about:blank"

fun createOnRestoring(
        initialUrl: String,
        private: Boolean,
        source: Source,
        id: String
): Session {
    return Session(initialUrl, private, source, id)
}

var Session.favicon: Bitmap?
    get() = getOrPutExtension(this).favicon
    set(value) {
        getOrPutExtension(this).favicon = value
    }

fun Session.isFromExternal(): Boolean {
    return this.parentId == ID_EXTERNAL
}

fun Session.setParentId(id: String?) {
    getOrPutExtension(this).extParentId = id
}

fun Session.getParentId(): String? {
    return getOrPutExtension(this).extParentId
}

fun Session.hasParentTab(): Boolean {
    return !isFromExternal() && !TextUtils.isEmpty(parentId)
}

fun Session.isValid(): Boolean {
    return this.id.isNotBlank() && (url?.isNotBlank() ?: false)
}

private val extensions = WeakHashMap<Session, SessionExtension>()

private fun getOrPutExtension(session: Session): SessionExtension {
    extensions[session]?.let { return it }

    return SessionExtension().also {
        extensions[session] = it
    }
}

private class SessionExtension {
    var extParentId: String? = null
    var favicon: Bitmap? = null
}
