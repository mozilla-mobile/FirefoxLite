package org.mozilla.rocket.persistance.History;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.history.model.Site;

import java.util.List;

public class HistoryRepository {

    private HistoryDatabase historyDatabase;

    public HistoryRepository(final HistoryDatabase database) {
        historyDatabase = database;
    }

    @NotNull
    public List<Site> searchHistory(@NotNull String text, int suggestionLimit) {
        return historyDatabase.historyDao().queryHistoryByText(text, suggestionLimit);
    }
}
