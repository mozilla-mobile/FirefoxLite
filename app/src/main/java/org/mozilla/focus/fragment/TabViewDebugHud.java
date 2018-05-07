package org.mozilla.focus.fragment;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;

import org.mozilla.focus.BuildConfig;

public class TabViewDebugHud extends android.support.v7.widget.AppCompatTextView {
    private WebView target;

    public static TabViewDebugHud create(Context context, @NonNull ViewGroup container,
                                         @Nullable View switchView) {
        TabViewDebugHud hud = BuildConfig.DEBUG ? new TabViewDebugHud(context) : new NoOpHud(context);
        hud.attach(container, switchView);
        return hud;
    }

    private TabViewDebugHud(Context context) {
        super(context);
        setBackgroundColor(Color.parseColor("#bbeebb"));
        setAlpha(0.9f);
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        setTextColor(Color.BLACK);
        setVisibility(View.GONE);
    }

    public void bind(Object target) {
        if (!isSupport() || !(target instanceof WebView)) {
            return;
        }

        this.target = (WebView) target;
        update();
    }

    public void update() {
        if (!isSupport() || target == null) {
            return;
        }

        WebBackForwardList list = target.copyBackForwardList();

        StringBuilder builder = new StringBuilder();
        builder.append("size: ").append(list.getSize()).append("\n");
        int spanStart = 0, spanEnd = 0, urlStart = 0;

        for (int i = 0; i < list.getSize(); ++i) {
            WebHistoryItem item = list.getItemAtIndex(i);
            if (item == null) {
                continue;
            }

            String prefix = "    " + (i + 1) + ". ";
            String line = prefix + makeLine(item);
            if (list.getCurrentIndex() == i) {
                spanStart = builder.length();
                urlStart = spanStart + prefix.length();
                spanEnd = spanStart + line.length();
            }
            builder.append(line).append("\n");
        }

        String result = builder.toString();
        Spannable spanText = new SpannableString(result);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(
                result.substring(urlStart, spanEnd).startsWith("data:") ? Color.RED : Color.BLUE);
        spanText.setSpan(colorSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        setText(spanText);
    }

    private void attach(@NonNull ViewGroup parentView, @Nullable View switchView) {
        if (!isSupport() || switchView == null) {
            return;
        }

        switchView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setVisibility(getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                return true;
            }
        });
        parentView.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private String makeLine(WebHistoryItem item) {
        final int maxLineLength = 60;
        String url = item.getOriginalUrl();
        String line = TextUtils.isEmpty(url) ? "" : url;
        return line.substring(0, Math.min(maxLineLength, line.length()));
    }

    protected boolean isSupport() {
        return true;
    }

    private static class NoOpHud extends TabViewDebugHud {
        public NoOpHud(Context context) {
            super(context);
            setVisibility(View.GONE);
        }

        @Override
        protected boolean isSupport() {
            return false;
        }
    }
}
