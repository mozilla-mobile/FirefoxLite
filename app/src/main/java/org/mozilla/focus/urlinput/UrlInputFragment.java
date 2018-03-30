/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.ScreenNavigator;
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FlowLayout;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.InlineAutocompleteEditText;

import java.util.List;
import java.util.Locale;

/**
 * Fragment for displaying he URL input controls.
 */
public class UrlInputFragment extends Fragment implements UrlInputContract.View,
        View.OnClickListener, View.OnLongClickListener,
        InlineAutocompleteEditText.OnCommitListener, InlineAutocompleteEditText.OnFilterListener,
        InlineAutocompleteEditText.OnTextChangeListener {

    public static final String FRAGMENT_TAG = "url_input";

    private static final String ARGUMENT_URL = "url";
    private static final String ARGUMENT_PARENT_FRAGMENT = "parent_frag_tag";

    private static final int REQUEST_THROTTLE_THRESHOLD = 300;

    private UrlInputContract.Presenter presenter;

    /**
     * Create a new UrlInputFragment and animate the url input view from the position/size of the
     * fake url bar view.
     */
    public static UrlInputFragment create(@Nullable String url, String parentFragmentTag) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);
        arguments.putString(ARGUMENT_PARENT_FRAGMENT, parentFragmentTag);

        UrlInputFragment fragment = new UrlInputFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private InlineAutocompleteEditText urlView;
    private FlowLayout suggestionView;
    private View clearView;

    private UrlAutoCompleteFilter urlAutoCompleteFilter;
    private boolean autoCompleteInProgress;
    private View dismissView;
    private long lastRequestTime;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final String userAgent = WebViewProvider.getUserAgentString(getActivity());
        this.presenter = new UrlInputPresenter(SearchEngineManager.getInstance()
                .getDefaultSearchEngine(getActivity()), userAgent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_urlinput, container, false);

        dismissView = view.findViewById(R.id.dismiss);
        dismissView.setOnClickListener(this);

        clearView = view.findViewById(R.id.clear);
        clearView.setOnClickListener(this);

        urlAutoCompleteFilter = new UrlAutoCompleteFilter();
        urlAutoCompleteFilter.loadDomainsInBackground(getContext().getApplicationContext());

        suggestionView = (FlowLayout) view.findViewById(R.id.search_suggestion);

        urlView = (InlineAutocompleteEditText) view.findViewById(R.id.url_edit);
        urlView.setOnTextChangeListener(this);
        urlView.setOnFilterListener(this);
        urlView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Avoid showing keyboard again when returning to the previous page by back key.
                if (hasFocus) {
                    ViewUtils.showKeyboard(urlView);
                } else {
                    ViewUtils.hideKeyboard(urlView);
                }
            }
        });

        urlView.setOnCommitListener(this);

        initByArguments();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.setView(this);
        urlView.requestFocus();

        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STARTED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.setView(null);
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STOPPED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.suggestion_item:
                setUrlText(((TextView) view).getText());
                TelemetryWrapper.searchSuggestionLongClick();
                return true;
            case R.id.clear:
                TelemetryWrapper.searchClear();
                return false;
            case R.id.dismiss:
                TelemetryWrapper.searchDismiss();
                return false;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear:
                urlView.setText("");
                urlView.requestFocus();
                break;
            case R.id.dismiss:
                dismiss();
                break;
            case R.id.suggestion_item:
                onSuggestionClicked(((TextView) view).getText());
                break;
            default:
                throw new IllegalStateException("Unhandled view in onClick()");
        }
    }

    private void initByArguments() {
        final Bundle args = getArguments();
        if (args.containsKey(ARGUMENT_URL)) {
            final String url = args.getString(ARGUMENT_URL);
            urlView.setText(url);
            clearView.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        }
    }

    private void onSuggestionClicked(CharSequence tag) {
        setUrlText(tag);
        onCommit(true);
    }

    private void dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, FragmentListener.TYPE.DISMISS_URL_INPUT, true);
        }
    }

    public void onCommit(boolean isSuggestion) {
        final String input = isSuggestion ? urlView.getOriginalText() : urlView.getText().toString();
        if (!input.trim().isEmpty()) {
            ViewUtils.hideKeyboard(urlView);

            final boolean isUrl = UrlUtils.isUrl(input);

            final String url = isUrl
                    ? UrlUtils.normalize(input)
                    : UrlUtils.createSearchUrl(getContext(), input);

            boolean isOpenInNewTab = openUrl(url);

            if (isOpenInNewTab) {
                TelemetryWrapper.addNewTabFromHome();
            }
            TelemetryWrapper.urlBarEvent(isUrl, isSuggestion);
        }
    }

    /**
     *
     * @param url the URL to open
     * @return true if open URL in new tab.
     */
    private boolean openUrl(String url) {
        boolean openNewTab = false;

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARGUMENT_PARENT_FRAGMENT)) {
            openNewTab = HomeFragment.FRAGMENT_TAG.equals(args.getString(ARGUMENT_PARENT_FRAGMENT));
        }
        ScreenNavigator.get(getContext()).showBrowserScreen(url, openNewTab, false);

        return openNewTab;
    }

    @Override
    public void setUrlText(CharSequence text) {
        this.urlView.setOnTextChangeListener(null);
        this.urlView.setText(text);
        this.urlView.setSelection(text.length());
        this.urlView.setOnTextChangeListener(this);
    }

    @Override
    public void setSuggestions(@Nullable List<CharSequence> texts) {
        this.suggestionView.removeAllViews();
        if (texts == null) {
            return;
        }

        final String searchKey = urlView.getOriginalText().trim().toLowerCase(Locale.getDefault());
        for (int i = 0; i < texts.size(); i++) {
            final TextView item = (TextView) View.inflate(getContext(), R.layout.tag_text, null);
            final String str = texts.get(i).toString();
            final int idx = str.toLowerCase(Locale.getDefault()).indexOf(searchKey);
            if (idx != -1) {
                SpannableStringBuilder builder = new SpannableStringBuilder(texts.get(i));
                builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx,
                        idx + searchKey.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                item.setText(builder);
            } else {
                item.setText(texts.get(i));
            }

            item.setOnClickListener(this);
            item.setOnLongClickListener(this);
            this.suggestionView.addView(item);
        }
    }

    @Override
    public void onFilter(String searchText, InlineAutocompleteEditText view) {
        // If the UrlInputFragment has already been hidden, don't bother with filtering. Because of the text
        // input architecture on Android it's possible for onFilter() to be called after we've already
        // hidden the Fragment, see the relevant bug for more background:
        // https://github.com/mozilla-mobile/focus-android/issues/441#issuecomment-293691141
        if (!isVisible()) {
            return;
        }
        autoCompleteInProgress = true;
        urlAutoCompleteFilter.onFilter(searchText, view);
        autoCompleteInProgress = false;
    }

    @Override
    public void onTextChange(String originalText, String autocompleteText) {
        if (autoCompleteInProgress) {
            return;
        }
        UrlInputFragment.this.presenter.onInput(originalText, detectThrottle());
        final int visibility = TextUtils.isEmpty(originalText) ? View.GONE : View.VISIBLE;
        UrlInputFragment.this.clearView.setVisibility(visibility);
    }

    private boolean detectThrottle() {
        long now = System.currentTimeMillis();
        boolean throttled = now - lastRequestTime < REQUEST_THROTTLE_THRESHOLD;
        lastRequestTime = now;
        return throttled;
    }
}
