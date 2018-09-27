/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.webkit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mozilla.focus.BuildConfig;
import org.mozilla.urlutils.UrlUtils;

public class WebViewDebugOverlay {
    private WebView webView;
    private LinearLayout backForwardList;
    private LinearLayout callbackList;
    private LinearLayout viewTreeList;

    private DrawerLayout drawerLayout;

    private Runnable viewTreeUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (webView != null && webView.isAttachedToWindow()) {
                viewTreeList.removeAllViews();
                updateViewTreeList(webView, 0);
            }
        }
    };

    @NonNull
    public static WebViewDebugOverlay create(Context context) {
        if (isSupport()) {
            return new WebViewDebugOverlay(context);
        }
        return new NoOpOverlay(context);
    }

    public static boolean isSupport() {
        return BuildConfig.DEBUG;
    }

    private WebViewDebugOverlay(Context context) {
        if (isEnable()) {
            // Init root layout
            final LinearLayout panelLayout = new LinearLayout(context);
            panelLayout.setOrientation(LinearLayout.VERTICAL);
            panelLayout.setBackgroundColor(Color.parseColor("#99000000"));

            insertSectionTitle("WebViewClient", panelLayout);
            panelLayout.addView(createCallbackList(context), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            insertSectionTitle("BackForwardList", panelLayout);
            panelLayout.addView(createBackForwardList(context), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            insertSectionTitle("View tree", panelLayout);
            panelLayout.addView(createViewTreeList(context), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // Init DrawerLayout
            drawerLayout = new FullScreenDrawerLayout(context);
            drawerLayout.setScrimColor(Color.TRANSPARENT);

            View ghostView = new View(context) {
                View target;
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    // FIXME: Bad touch event dispatching, refine this
                    if (target != null) {
                        boolean handled = target.onTouchEvent(event);
                        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                            target = null;
                        }
                        return handled;
                    }

                    if (webView.getChildCount() != 0) {
                        for (int i = 0; i < webView.getChildCount(); ++i) {
                            View child = webView.getChildAt(i);
                            if (child == drawerLayout) {
                                continue;
                            }

                            boolean handled = child.onTouchEvent(event);
                            if (handled) {
                                target = child;
                                return true;
                            }
                        }
                    }
                    return webView.onTouchEvent(event);
                }
            };
            drawerLayout.addView(ghostView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            DrawerLayout.LayoutParams params = new DrawerLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.START;
            panelLayout.setLayoutParams(params);
            drawerLayout.addView(panelLayout);
        }
    }

    public void onWebViewScrolled(int left, int top) {
        if (isEnable()) {
            drawerLayout.setTranslationY(top);
            drawerLayout.setTranslationX(left);
        }
    }

    public void updateHistory() {
        if (isEnable()) {
            backForwardList.removeAllViews();
            WebBackForwardList list = this.webView.copyBackForwardList();
            int size = list.getSize();
            int curr = list.getCurrentIndex();
            if (curr < 0) {
                return;
            }
            insertHistory("size:" + size + ", curr:" + curr, Color.WHITE);

            int first = curr - 2;
            int last = curr + 2;
            if (first < 0) {
                last -= first;
                first = 0;
            }
            if (last >= size) {
                first = Math.max(0, first - (last - size + 1));
                last = size - 1;
            }

            if (first != 0) {
                insertHistory("...", Color.WHITE);
            }

            for (int i = first; i <= last; ++i) {
                WebHistoryItem item = list.getItemAtIndex(i);
                String line = item.getOriginalUrl().replaceAll("https://", "");
                String line2 = item.getUrl().replaceAll("https://", "");

                int color;
                if (list.getCurrentIndex() == i) {
                    color = UrlUtils.isInternalErrorURL(item.getOriginalUrl())
                            || UrlUtils.isInternalErrorURL(item.getUrl())
                            ? Color.RED : Color.GREEN;
                } else {
                    color = Color.LTGRAY;
                }
                insertHistory(i + ": " + line, color);
                if (!line.equals(line2)) {
                    insertHistory("-> " + line2, color);
                }
                insertDivider(backForwardList);
            }

            updateViewTree();
        }
    }

    public void recordLifecycle(String name, boolean isPageStart) {
        if (isEnable()) {
            while (callbackList.getChildCount() + 1 > 10) {
                callbackList.removeViewAt(0);
            }

            if (isPageStart && callbackList.getChildCount() != 0) {
                insertDivider(callbackList);
            }
            insertCallback(name.replace("https://", ""), Color.LTGRAY);
        }
    }

    public void onLoadUrlCalled() {
        if (isEnable()) {
            callbackList.removeAllViews();
        }
    }

    public boolean isEnable() {
        return true;
    }

    public void bindWebView(final WebView webView) {
        if (isEnable()) {
            this.webView = webView;
            webView.addView(drawerLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            updateHistory();
            drawerLayout.setElevation(100);
        }
    }

    private void updateViewTree() {
        if (isEnable()) {
            webView.removeCallbacks(viewTreeUpdateRunnable);
            viewTreeList.removeAllViews();
            insertText("updating...", Color.LTGRAY, viewTreeList);
            webView.postDelayed(viewTreeUpdateRunnable, 1500);
        }
    }

    private void updateViewTreeList(View rootView, int level) {
        if (!(rootView instanceof ViewGroup)) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        while (count < level) {
            if (count < level - 1) {
                builder.append("  ");
                count += 1;
            } else if (count == level - 1) {
                builder.append("|-");
                count += 2;
            } else {
                builder.append("--");
                count += 2;
            }
        }
        builder.append(rootView.getClass().getSimpleName());
        if (rootView instanceof WebView) {
            WebHistoryItem item = ((WebView) rootView).copyBackForwardList().getCurrentItem();
            if (item != null) {
                builder.append("(").append(item.getOriginalUrl()).append(")");
            }
        }
        insertText(builder.toString(), Color.LTGRAY, viewTreeList);

        if (rootView == viewTreeList) {
            return;
        }

        ViewGroup viewGroup = (ViewGroup) rootView;
        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
            updateViewTreeList(viewGroup.getChildAt(i), level + 1);
        }
    }

    private View createCallbackList(Context context) {
        callbackList = new LinearLayout(context);
        callbackList.setOrientation(LinearLayout.VERTICAL);
        return callbackList;
    }

    private View createBackForwardList(Context context) {
        backForwardList = new LinearLayout(context);
        backForwardList.setOrientation(LinearLayout.VERTICAL);
        return backForwardList;
    }

    private View createViewTreeList(Context context) {
        viewTreeList = new LinearLayout(context);
        viewTreeList.setOrientation(LinearLayout.VERTICAL);
        return viewTreeList;
    }

    private void insertSectionTitle(String name, LinearLayout layout) {
        if (isEnable()) {
            TextView view = new TextView(layout.getContext());
            view.setText(name);
            view.setTextColor(Color.WHITE);
            view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            view.setTypeface(Typeface.MONOSPACE);
            float density = view.getResources().getDisplayMetrics().density;
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = (layout.getChildCount() == 0) ? 0 : (int) (density * 16);
            layout.addView(view, params);
        }
    }

    private void insertHistory(String history, int textColor) {
        insertText(history, textColor, backForwardList);
    }

    private void insertCallback(String lifecycle, int textColor) {
        insertText(lifecycle, textColor, callbackList);
    }

    private void insertText(String text, int textColor, LinearLayout parent) {
        AppCompatTextView textView = new AppCompatTextView(callbackList.getContext());
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView.setText(text);
        textView.setMaxLines(1);
        textView.setSingleLine();
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTypeface(Typeface.MONOSPACE);
        parent.addView(textView);
    }

    private void insertDivider(LinearLayout layout) {
        View v = new View(webView.getContext());
        v.setBackgroundColor(Color.WHITE);
        layout.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private static class NoOpOverlay extends WebViewDebugOverlay {
        private NoOpOverlay(Context context) {
            super(context);
        }

        @Override
        public boolean isEnable() {
            return false;
        }
    }

    private static class FullScreenDrawerLayout extends DrawerLayout {

        public FullScreenDrawerLayout(@NonNull Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
                throw new IllegalArgumentException("DrawerLayout must be measured with MeasureSpec.EXACTLY.");
            }

            setMeasuredDimension(widthSize, heightSize);

            boolean hasDrawerOnLeftEdge = false;
            boolean hasDrawerOnRightEdge = false;
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);

                if (child.getVisibility() == GONE) {
                    continue;
                }

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                if (isContentView(child)) {
                    // Content views get measured at exactly the layout's size.
                    final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                            widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                    final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                            heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);
                } else if (isDrawerView(child)) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        int elevation = (int) (10 * getResources().getDisplayMetrics().density);
                        if ((int) ViewCompat.getElevation(child) != elevation) {
                            ViewCompat.setElevation(child, elevation);
                        }
                    }
                    final int childGravity =
                            getDrawerViewAbsoluteGravity(child) & Gravity.HORIZONTAL_GRAVITY_MASK;
                    // Note that the isDrawerView check guarantees that childGravity here is either
                    // LEFT or RIGHT
                    boolean isLeftEdgeDrawer = (childGravity == Gravity.LEFT);
                    if ((isLeftEdgeDrawer && hasDrawerOnLeftEdge)
                            || (!isLeftEdgeDrawer && hasDrawerOnRightEdge)) {
                        throw new IllegalStateException("Duplicate drawers on the same edge");
                    }
                    if (isLeftEdgeDrawer) {
                        hasDrawerOnLeftEdge = true;
                    } else {
                        hasDrawerOnRightEdge = true;
                    }
                    final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                            lp.leftMargin + lp.rightMargin,
                            lp.width);
                    final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                            lp.topMargin + lp.bottomMargin,
                            lp.height);
                    child.measure(drawerWidthSpec, drawerHeightSpec);
                } else {
                    throw new IllegalStateException("Child " + child + " at index " + i
                            + " does not have a valid layout_gravity - must be Gravity.LEFT, "
                            + "Gravity.RIGHT or Gravity.NO_GRAVITY");
                }
            }
        }

        boolean isContentView(View child) {
            return ((LayoutParams) child.getLayoutParams()).gravity == Gravity.NO_GRAVITY;
        }

        boolean isDrawerView(View child) {
            final int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
            final int absGravity = GravityCompat.getAbsoluteGravity(gravity,
                    ViewCompat.getLayoutDirection(child));
            return (absGravity & Gravity.LEFT) != 0 || (absGravity & Gravity.RIGHT) != 0;
        }

        int getDrawerViewAbsoluteGravity(View drawerView) {
            final int gravity = ((LayoutParams) drawerView.getLayoutParams()).gravity;
            return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));
        }
    }
}
