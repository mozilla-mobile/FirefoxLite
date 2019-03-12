package org.mozilla.focus.firstrun;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.InfoActivity;
import org.mozilla.focus.utils.SupportUtils;

public class FirstRunLibrary {

    public static FirstrunPage buildLifeFeedFirstrun(Context context) {
        final String feedURL = SupportUtils.getSumoURLForTopic(context, "firefox-lite-feed");
        final String lifeFeed = context.getString(R.string.life_feed);
        final String learnMore = context.getString(R.string.about_link_learn_more);
        final String content = context.getString(R.string.first_run_page6_text, lifeFeed, "%s");
        final Spannable link = linkTextSpan(context, content, learnMore, feedURL, lifeFeed);
        return new FirstrunPage(
                context.getString(R.string.first_run_page6_title),
                link,
                R.drawable.onboarding_lifefeed);
    }

    @NonNull
    static private Spannable linkTextSpan(Context context, String body, String link, String url, String title) {
        final String content = String.format(body, link);
        int start = content.indexOf(link);
        int end = start + link.length();
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                context.startActivity(InfoActivity.getIntentFor(context, url, title));
            }
        };
        final SpannableStringBuilder linkSpan = new SpannableStringBuilder(content);
        linkSpan.setSpan(clickableSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return linkSpan;
    }
}
