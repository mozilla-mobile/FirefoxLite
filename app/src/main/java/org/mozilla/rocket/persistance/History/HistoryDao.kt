package org.mozilla.rocket.persistance.History

import androidx.room.Dao
import androidx.room.Query
import org.mozilla.focus.history.model.Site

@Dao
interface HistoryDao {
    @Query("SELECT * FROM browsing_history WHERE LOWER(title) LIKE :text OR url LIKE :text LIMIT :suggestionLimit")
    fun queryHistoryByText(
        text: String?,
        suggestionLimit: Int
    ): List<Site>
}
