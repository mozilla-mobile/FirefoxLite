package org.mozilla.rocket.tabs.ext

import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.webkit.GeolocationPermissions
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.Session.Source
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.rocket.tabs.EngineSessionHolder
import org.mozilla.rocket.tabs.TabView
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

val Session.engineSessionHolder: EngineSessionHolder
    get() = getOrPutExtension(this).engineSessionHolder


fun Session.isFromExternal(): Boolean {
    return this.getParentId() == ID_EXTERNAL
}

fun Session.setParentId(id: String?) {
    getOrPutExtension(this).extParentId = id
}

fun Session.getParentId(): String? {
    return getOrPutExtension(this).extParentId
}

fun Session.hasParentTab(): Boolean {
    return !isFromExternal() && !TextUtils.isEmpty(this.getParentId())
}

fun Session.isValid(): Boolean {
    return this.id.isNotBlank() && (url?.isNotBlank() ?: false)
}

fun Session.registerExt(ext: SessionExtension.Observer) {
    getOrPutExtension(this).extObservers.register(ext)
}

fun Session.unregisterExt(ext: SessionExtension.Observer) {
    getOrPutExtension(this).extObservers.unregister(ext)
}

fun Session.notifyObserversExt(block: SessionExtension.Observer.() -> Unit) {
    getOrPutExtension(this).extObservers.notifyObservers(block)
}

private val extensions = WeakHashMap<Session, SessionExtension>()

private fun getOrPutExtension(session: Session): SessionExtension {
    extensions[session]?.let { return it }

    return SessionExtension().also {
        extensions[session] = it
    }
}

class SessionExtension {
    var extParentId: String? = null
    var favicon: Bitmap? = null
    val extObservers: ObserverRegistry<Observer> = ObserverRegistry()

    /**
     * Holder for keeping a reference to an engine session and its observer to update this session
     * object.
     */
    val engineSessionHolder = EngineSessionHolder()

    interface Observer : Session.Observer {
        fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback?
        ) = Unit

        /**
         * Notify the host application that the current page has entered full screen mode.
         * <p>
         * The callback needs to be invoked to request the page to exit full screen mode.
         * <p>
         * Some TabView implementations may pass a custom View which contains the web contents in
         * full screen mode.
         */
        fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) = Unit

        /**
         * Notify the host application that the current page has exited full screen mode.
         * <p>
         * If a View was passed when the application entered full screen mode then this view must
         * be hidden now.
         */
        fun onExitFullScreen() = Unit

        fun onReceivedIcon(icon: Bitmap?) = Unit
        fun onLongPress(session: Session, hitTarget: TabView.HitTarget) = Unit
    }
}

