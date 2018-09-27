/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.os.AsyncTask;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.httptask.SimpleLoadUrlTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UrlInputPresenter implements UrlInputContract.Presenter {

    private UrlInputContract.View view;
    final private SearchEngine searchEngine;
    final private String userAgent;
    private static final int MAX_SUGGESTION_COUNT = 5;

    private AsyncTask queryTask;

    UrlInputPresenter(@NonNull SearchEngine searchEngine, String userAgent) {
        this.searchEngine = searchEngine;
        this.userAgent = userAgent;
    }

    @Override
    public void setView(UrlInputContract.View view) {
        this.view = view;
        // queryTask holds a WeakReference to view, cancel the task too.
        if (view == null && queryTask != null) {
            queryTask.cancel(false);
        }
    }

    @MainThread
    @Override
    public void onInput(@NonNull CharSequence input, boolean isThrottled) {
        if (isThrottled && queryTask != null) {
            queryTask.cancel(true);
        }
        if (view == null) {
            return;
        }

        if (input.length() == 0) {
            this.view.setSuggestions(null);
            return;
        }

        // No need to provide suggestion for Url input
        if (SupportUtils.isUrl(input.toString())) {
            return;
        }

        if (queryTask != null) {
            queryTask.cancel(true);
            queryTask = null;
        }

        queryTask = new QueryTask(view).execute(searchEngine.buildSearchSuggestionUrl(input.toString()), userAgent, Integer.toString(SocketTags.SEARCH_SUGGESTION));


    }

    private static class QueryTask extends SimpleLoadUrlTask {

        private WeakReference<UrlInputContract.View> viewWeakReference;

        QueryTask(UrlInputContract.View view) {
            viewWeakReference = new WeakReference<>(view);
        }

        @Override
        protected void onPostExecute(String line) {
            if (TextUtils.isEmpty(line)) {
                return;
            }
            List<CharSequence> suggests = null;
            try {
                JSONArray response = new JSONArray(line);
                JSONArray suggestions = response.getJSONArray(1);
                int size = suggestions.length();
                suggests = new ArrayList<>(size);

                for (int i = 0; i < Math.min(size, MAX_SUGGESTION_COUNT); i++) {
                    suggests.add(suggestions.getString(i));
                }
            } catch (JSONException ignored) {
            } finally {
                if (suggests == null) {
                    suggests = Collections.emptyList();
                }
            }

            UrlInputContract.View view = viewWeakReference.get();
            if (view != null) {
                view.setSuggestions(suggests);
            }
        }
    }
}
