/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.http.SslError;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.TextView;

import org.mozilla.focus.BuildConfig;

public class ErrorPageDelegate {
    private final ErrorViewFactory<HtmlErrorViewHolder> factory;

    ErrorPageDelegate(final WebView webView) {
        factory = new ErrorViewFactory<HtmlErrorViewHolder>() {
            @Override
            public HtmlErrorViewHolder onCreateErrorView() {
                Context context = webView.getContext();
                WebView errorView = new WebView(context);

                if (BuildConfig.DEBUG) {
                    TextView textView = new TextView(context);
                    textView.setTextColor(Color.RED);
                    textView.setText(View.class.getCanonicalName());
                    errorView.addView(textView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }

                HtmlErrorViewHolder holder = new HtmlErrorViewHolder(errorView);
                holder.webView = errorView;
                return holder;
            }

            @Override
            public void onBindErrorView(HtmlErrorViewHolder viewHolder, int errorCode, String description, String failingUrl) {
                ErrorPage.loadErrorPage(viewHolder.webView, failingUrl, errorCode);
                bindRetryButton(viewHolder);
            }

            @Override
            public void onBindSslErrorView(HtmlErrorViewHolder viewHolder, SslErrorHandler handler, SslError error) {
                WebBackForwardList list = webView.copyBackForwardList();
                WebHistoryItem currentItem = list.getCurrentItem();
                if (currentItem != null) {
                    webView.removeView(viewHolder.rootView);
                    ErrorPage.loadErrorPage(webView, error.getUrl(), error.getPrimaryError());
                }
            }

            @Override
            public void onBindHttpErrorView(HtmlErrorViewHolder viewHolder, WebResourceRequest request, WebResourceResponse errorResponse) {
                ErrorPage.loadErrorPage(viewHolder.webView, request.getUrl().toString(), errorResponse.getStatusCode());
                bindRetryButton(viewHolder);
            }

            @Override
            public void onErrorViewCreated(HtmlErrorViewHolder viewHolder) {
                webView.addView(viewHolder.rootView, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onErrorViewDestroyed(HtmlErrorViewHolder viewHolder) {
                if (viewHolder.webView != null) {
                    webView.removeView(viewHolder.rootView);
                    viewHolder.webView.removeJavascriptInterface(JsInterface.NAME);
                    viewHolder.webView.destroy();
                }
            }


            // Web view here is used only for local error page, should have no security problem.
            // thus suppressing the lint
            @SuppressLint("SetJavaScriptEnabled")
            private void bindRetryButton(HtmlErrorViewHolder viewHolder) {
                viewHolder.webView.getSettings().setJavaScriptEnabled(true);
                viewHolder.webView.addJavascriptInterface(new JsInterface() {
                    @Override
                    void onReloadOnJsThread() {
                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                webView.reload();
                            }
                        });
                    }
                }, JsInterface.NAME);
            }
        };
    }

    private abstract static class JsInterface {
        private static final String NAME = "jsInterface";

        abstract void onReloadOnJsThread();

        @JavascriptInterface
        public void onRetryClicked() {
            onReloadOnJsThread();
        }
    }

    public void onPageStarted() {
        dismissErrorView();
    }

    public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
        createErrorView();
        factory.onBindErrorView(factory.getErrorViewHolder(), errorCode, description, failingUrl);
    }

    public void onReceivedSslError(WebView webView, SslErrorHandler handler, SslError error) {
        createErrorView();
        factory.onBindSslErrorView(factory.getErrorViewHolder(), handler, error);
    }

    public void onReceivedHttpError(WebView webView, WebResourceRequest request, WebResourceResponse errorResponse) {
        // Currently we are not going to show error page for http error
        //createErrorView();
        //factory.onBindHttpErrorView(factory.getErrorViewHolder(), request, errorResponse);
    }

    private void createErrorView() {
        factory.createErrorView();
    }

    private void dismissErrorView() {
        factory.destroyErrorView();
    }

    public void onWebViewScrolled(int left, int top) {
        ErrorViewHolder holder = factory.getErrorViewHolder();
        if (holder != null) {
            holder.rootView.setTranslationX(left);
            holder.rootView.setTranslationY(top);
        }
    }

    public static abstract class ErrorViewFactory<T extends ErrorViewHolder> {
        private T errorViewHolder;

        public abstract T onCreateErrorView();

        public abstract void onBindErrorView(T viewHolder, int errorCode, String description, String failingUrl);
        public abstract void onBindSslErrorView(T viewHolder, SslErrorHandler handler, SslError error);
        public abstract void onBindHttpErrorView(T viewHolder, WebResourceRequest request, WebResourceResponse errorResponse);

        public abstract void onErrorViewCreated(T viewHolder);
        public abstract void onErrorViewDestroyed(T viewHolder);

        private void createErrorView() {
            if (errorViewHolder != null) {
                destroyErrorView();
            }
            onErrorViewCreated(errorViewHolder = onCreateErrorView());
        }

        private void destroyErrorView() {
            if (errorViewHolder != null) {
                onErrorViewDestroyed(errorViewHolder);
            }
            errorViewHolder = null;
        }

        private T getErrorViewHolder() {
            return errorViewHolder;
        }
    }

    public static class ErrorViewHolder {
        public View rootView;
        ErrorViewHolder(View rootView) {
            this.rootView = rootView;
        }
    }

    public static class HtmlErrorViewHolder extends ErrorViewHolder {
        WebView webView;

        HtmlErrorViewHolder(View rootView) {
            super(rootView);
        }
    }
}
