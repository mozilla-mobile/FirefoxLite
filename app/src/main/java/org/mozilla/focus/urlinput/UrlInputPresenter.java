/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.net.TrafficStats;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.mozilla.focus.network.SocketTags;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.utils.UrlUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UrlInputPresenter implements UrlInputContract.Presenter {

    private UrlInputContract.View view;
    final private SearchEngine searchEngine;
    final private String userAgent;
    private static final int MAX_SUGGESTION_COUNT = 5;

    private AsyncTask queryTask;

    public UrlInputPresenter(@NonNull SearchEngine searchEngine, String userAgent) {
        this.searchEngine = searchEngine;
        this.userAgent = userAgent;
    }

    @Override
    public void setView(UrlInputContract.View view) {
        queryTask.cancel(true);
        queryTask = null;
        this.view = view;
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
        if (UrlUtils.isUrl(input.toString())) {
            return;
        }

        if (queryTask != null) {
            queryTask.cancel(true);
            queryTask = null;
        }

        queryTask = new QueryTask(view).execute(searchEngine.buildSearchSuggestionUrl(input.toString()), userAgent);


    }

    private static class QueryTask extends AsyncTask<String, Void, List<CharSequence>> {

        private WeakReference<UrlInputContract.View> viewWeakReference;

        QueryTask(UrlInputContract.View view) {
            viewWeakReference = new WeakReference<>(view);
        }

        @Override
        protected List<CharSequence> doInBackground(String... strings) {
            TrafficStats.setThreadStatsTag(SocketTags.SEARCH_SUGGESTION);
            try {
                return HttpRequest.get(new URL(strings[0]), strings[1]);
            } catch (MalformedURLException ex) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<CharSequence> strings) {
            UrlInputContract.View view = viewWeakReference.get();
            if (view != null && strings != null) {
                view.setSuggestions(strings);
            }
        }
    }

    private static class HttpRequest {

        static List<CharSequence> get(URL url, final String userAgent) {

            String line = "";
            HttpURLConnection urlConnection = null;
            BufferedReader r = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", userAgent);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                r = new BufferedReader(new InputStreamReader(in, "utf-8"));
                StringBuilder total = new StringBuilder();
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }

                line = total.toString();
            } catch (IOException e) {
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (Exception e) {
                        ;
                    }
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            if (TextUtils.isEmpty(line)) {
                return Collections.emptyList();
            }

            List<CharSequence> suggests = null;
            try {
                JSONArray response = new JSONArray(line);
                JSONArray suggestions = response.getJSONArray(1);
                int size = suggestions.length();
                suggests = new ArrayList<>(size);
                try {
                    for (int i = 0; i < Math.min(size, MAX_SUGGESTION_COUNT); i++) {
                        suggests.add(suggestions.getString(i));
                    }
                } catch (JSONException e) {
                }
            } catch (JSONException e) {
            } finally {
                if (suggests == null) {
                    suggests = Collections.emptyList();
                }
            }

            return suggests;
        }
    }
}
