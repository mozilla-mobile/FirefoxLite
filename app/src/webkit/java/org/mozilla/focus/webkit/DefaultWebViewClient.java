package org.mozilla.focus.webkit;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;

import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.UrlUtils;

public class DefaultWebViewClient extends TrackingProtectionWebViewClient {

    public DefaultWebViewClient(Context context) {
        super(context);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url) {
        webView.getSettings().setLoadsImagesAutomatically(true);

        if (shouldOverrideInternalPages(webView, url)) {
            return true;
        }

        final Uri uri = Uri.parse(url);
        if (!UrlUtils.isSupportedProtocol(uri.getScheme()) &&
                IntentUtils.handleExternalUri(webView.getContext(), url)) {
            return true;
        }

        webView.getSettings().setLoadsImagesAutomatically(!Settings.getInstance(webView.getContext()).shouldBlockImages());
        return super.shouldOverrideUrlLoading(webView, url);
    }

    private boolean shouldOverrideInternalPages(WebView webView, String url) {
        if (SupportUtils.isTemplateSupportPages(url)) {
            SupportUtils.loadSupportPages(webView, url);
            return true;
        }
        return false;
    }
}
