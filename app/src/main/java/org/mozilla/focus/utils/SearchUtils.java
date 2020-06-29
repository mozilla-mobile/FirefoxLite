package org.mozilla.focus.utils;

import android.content.Context;

import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;

public class SearchUtils {

    public static String createSearchUrl(Context context, String searchTerm) {
        final SearchEngine searchEngine = SearchEngineManager.getInstance()
                .getDefaultSearchEngine(context);

        return searchEngine.buildSearchUrl(searchTerm);
    }

    public static String createSearchUrlWithSpecificSearchEngine(String searchEngineName, String searchTerm) {
        final SearchEngine searchEngine = SearchEngineManager.getInstance()
                .getSearchEngineWithName(searchEngineName);

        return searchEngine.buildSearchUrl(searchTerm);
    }
}
