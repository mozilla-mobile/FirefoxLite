package org.mozilla.rocket.awesomebar

import android.graphics.Bitmap
import mozilla.components.concept.awesomebar.AwesomeBar
import org.mozilla.rocket.tabs.SessionManager
import java.util.UUID

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on the sessions in the
 * [SessionManager] (Open tabs).
 */
class SessionSuggestionProvider(
    private val icon: Bitmap? = null,
    private val sessionManager: SessionManager,
    private val onSuggestionClicked: ((sessionManager: SessionManager, text: String) -> Unit)
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        return sessionManager.getTabs()
            .filter { it.url?.contains(text) ?: false || it.title.contains(text) }
            .distinctBy { it.url }
            .sortedBy { it.url }
            .map {
                AwesomeBar.Suggestion(
                    provider = this,
                    id = it.id,
                    icon = { _, _ -> icon },
                    title = it.title,
                    description = it.url,
                    onSuggestionClicked = { onSuggestionClicked.invoke(sessionManager, it.id) }
                )
            }
    }
}
