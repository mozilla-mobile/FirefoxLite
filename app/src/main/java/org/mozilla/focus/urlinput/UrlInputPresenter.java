/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.utils.UrlUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UrlInputPresenter implements UrlInputContract.Presenter {

    private UrlInputContract.View view;

    // This is just a Mock Presenter, in real implementation we should get rid of Android classes.
    final private Context ctx;

    private AsyncTask queryTask;

    public UrlInputPresenter(@NonNull Context context) {
        this.ctx = context;
    }

    @Override
    public void setView(UrlInputContract.View view) {
        this.view = view;
    }

    @MainThread
    @Override
    public void onInput(@NonNull CharSequence input) {
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

        queryTask = new AsyncTask<CharSequence, Void, List<CharSequence>>() {
            @Override
            protected List<CharSequence> doInBackground(CharSequence... urls) {
                return HttpRequest.get(urls[0]);
            }

            @Override
            protected void onPostExecute(List<CharSequence> strings) {
                if(view != null){
                    view.setSuggestions(strings);
                }
            }
        }.execute(input);

    }

    private static class HttpRequest {
        private static final String URL_QUERY_API_DUCKDUCKGO = "https://ac.duckduckgo.com/ac/?q=";

        static List<CharSequence> get(CharSequence uri) {

            String line = "";
            HttpURLConnection urlConnection = null;
            BufferedReader r = null;
            try {
                URL url = new URL(URL_QUERY_API_DUCKDUCKGO + uri);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                r = new BufferedReader(new InputStreamReader(in, "utf-8"));
                StringBuilder total = new StringBuilder();
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }

                line = total.toString();
            } catch (MalformedURLException e) {
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
                JSONArray jsonArray = new JSONArray(line);
                int size = jsonArray.length();
                suggests = new ArrayList<>(size);
                try {
                    for (int i = 0; i < size; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String suggestText = jsonObject.getString("phrase");
                        suggests.add(suggestText);
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
