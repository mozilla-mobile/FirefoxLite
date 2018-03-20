package org.mozilla.focus.firstrun;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.mozilla.focus.R;

public class FirstrunUpgradePagerAdapter extends PagerAdapter {

    private static class FirstrunPage {
        public final String title;
        public final String content1;
        public final String content1Highlight;
        public final String content2;
        public final String content2Highlight;

        private FirstrunPage(String title, String content1, String content1Highlight, String content2, String content2Highlight) {
            this.title = title;
            this.content1 = content1;
            this.content2 = content2;
            this.content1Highlight = content1Highlight;
            this.content2Highlight = content2Highlight;
        }
    }

    private final Context context;
    private final View.OnClickListener listener;
    private final FirstrunUpgradePagerAdapter.FirstrunPage[] pages;

    public FirstrunUpgradePagerAdapter(Context context, View.OnClickListener listener) {
        this.context = context;
        this.listener = listener;
        pages = new FirstrunUpgradePagerAdapter.FirstrunPage[]{
                new FirstrunPage(
                        context.getString(R.string.first_run_upgrade_page_title),
                        context.getString(R.string.first_run_upgrade_page_text1),
                        context.getString(R.string.first_run_upgrade_page_text1_highlight),
                        context.getString(R.string.first_run_upgrade_page_text2),
                        context.getString(R.string.first_run_upgrade_page_text2_highlight)
                )
        };
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getCount() {
        return pages.length;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ViewPager pager = (ViewPager) container;
        View view = getView(position, pager);

        pager.addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    private View getView(int position, ViewGroup container) {
        final View view = LayoutInflater.from(context).inflate(R.layout.firstrun_upgrade_page, container, false);

        FirstrunPage firstrunPages = pages[position];

        final StyleSpan styleSpan = new android.text.style.StyleSpan(android.graphics.Typeface.BOLD);
        final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourcesCompat.getColor(context.getResources(), R.color.onboarding_text_highlight, null));

        final TextView titleView = (TextView) view.findViewById(R.id.title);
        titleView.setText(firstrunPages.title);

        final Spannable highlightSpan1 = highlightTextSpan(firstrunPages.content1, firstrunPages.content1Highlight, styleSpan, foregroundColorSpan);
        final TextView text1View = (TextView) view.findViewById(R.id.text1);
        text1View.setText(highlightSpan1);

        final Spannable highlightSpan2 = highlightTextSpan(firstrunPages.content2, firstrunPages.content2Highlight, styleSpan, foregroundColorSpan);
        final TextView text2View = (TextView) view.findViewById(R.id.text2);
        text2View.setText(highlightSpan2);


        final Button buttonView = (Button) view.findViewById(R.id.button);
        buttonView.setOnClickListener(listener);
        buttonView.setText(R.string.firstrun_close_button);
        buttonView.setId(R.id.finish);

        return view;
    }

    @NonNull
    private Spannable highlightTextSpan(String body, String highlight, StyleSpan styleSpan, ForegroundColorSpan foregroundColorSpan) {
        final String content = String.format(body, highlight);
        int start = content.indexOf(highlight);
        int end = start + highlight.length();
        final SpannableStringBuilder highlightSpan = new SpannableStringBuilder(content);
        highlightSpan.setSpan(styleSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        highlightSpan.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return highlightSpan;
    }
}
