package org.mozilla.rocket.persistance.History;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.history.model.Site;

import java.util.List;

public class HistoryRepository {
    private static volatile HistoryRepository instance;

    private HistoryDatabase historyDatabase;

    private HistoryRepository(final HistoryDatabase database) {
        historyDatabase = database;
    }

    public static HistoryRepository getInstance(final HistoryDatabase database) {
        if (instance == null) {
            synchronized (HistoryRepository.class) {
                if (instance == null) {
                    instance = new HistoryRepository(database);
                }
            }
        }
        return instance;
    }

    @NotNull
    public List<Site> searchHistory(@NotNull String text, int suggestionLimit) {
        return historyDatabase.historyDao().queryHistoryByText(text, suggestionLimit);
    }
}
