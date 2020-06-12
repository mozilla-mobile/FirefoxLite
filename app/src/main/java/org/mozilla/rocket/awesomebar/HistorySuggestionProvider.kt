package org.mozilla.rocket.awesomebar

import android.graphics.Bitmap
import mozilla.components.concept.awesomebar.AwesomeBar
import org.mozilla.rocket.persistance.History.HistoryRepository
import java.util.UUID

class HistorySuggestionProvider(
    private val icon: Bitmap?,
    private val historyRepository: HistoryRepository,
    private val onSuggestionClicked: ((text: String) -> Unit)
) : AwesomeBar.SuggestionProvider {
    companion object {
        private const val TAG = "awesome_b_p"
        private const val SUGGESTION_LIMIT = 5
    }

    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        return historyRepository.searchHistory("%$text%", SUGGESTION_LIMIT)
                .distinctBy { it.url }
                .sortedBy { it.url }
                .map {
                    AwesomeBar.Suggestion(
                            provider = this,
                            id = it.id.toString(),
                            icon = { _, _ -> icon },
                            title = it.title,
                            description = it.url,
                            onSuggestionClicked = { onSuggestionClicked.invoke(it.url) }
                    )
                }
    }
}
