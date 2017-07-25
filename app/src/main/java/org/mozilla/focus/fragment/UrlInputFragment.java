/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.ThreadUtils;
import org.mozilla.focus.utils.UrlUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.widget.FragmentListener;
import org.mozilla.focus.widget.HintFrameLayout;
import org.mozilla.focus.widget.InlineAutocompleteEditText;

/**
 * Fragment for displaying he URL input controls.
 */
public class UrlInputFragment extends Fragment implements View.OnClickListener, InlineAutocompleteEditText.OnCommitListener, InlineAutocompleteEditText.OnFilterListener {
    public static final String FRAGMENT_TAG = "url_input";

    private static final String ARGUMENT_URL = "url";
    private static final String ARGUMENT_ANIMATION = "animation";
    private static final String ARGUMENT_X = "x";
    private static final String ARGUMENT_Y = "y";
    private static final String ARGUMENT_WIDTH = "width";
    private static final String ARGUMENT_HEIGHT = "height";

    private static final String ANIMATION_HOME_SCREEN = "home_screen";
    private static final String ANIMATION_BROWSER_SCREEN = "browser_screen";

    /**
     * Create a new UrlInputFragment and animate the url input view from the position/size of the
     * fake url bar view.
     */
    public static UrlInputFragment createWithHomeScreenAnimation(@Nullable View fakeUrlBarView,
                                                                 @Nullable String url) {
        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_URL, url);
        arguments.putString(ARGUMENT_ANIMATION, ANIMATION_HOME_SCREEN);

        int[] screenLocation = new int[2];
        if (fakeUrlBarView == null) {
            arguments.putInt(ARGUMENT_X, 0);
            arguments.putInt(ARGUMENT_Y, 0);
            arguments.putInt(ARGUMENT_WIDTH, 1);
            arguments.putInt(ARGUMENT_HEIGHT, 1);
        } else {
            fakeUrlBarView.getLocationOnScreen(screenLocation);
            arguments.putInt(ARGUMENT_X, screenLocation[0]);
            arguments.putInt(ARGUMENT_Y, screenLocation[1]);
            arguments.putInt(ARGUMENT_WIDTH, fakeUrlBarView.getWidth());
            arguments.putInt(ARGUMENT_HEIGHT, fakeUrlBarView.getHeight());
        }

        UrlInputFragment fragment = new UrlInputFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    /**
     * Create a new UrlInputFragment and animate the url input view from the position/size of the
     * browser toolbar's URL view.
     */
    public static UrlInputFragment createWithBrowserScreenAnimation(String url, View urlView) {
        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ANIMATION, ANIMATION_BROWSER_SCREEN);
        arguments.putString(ARGUMENT_URL, url);

        int[] screenLocation = new int[2];
        urlView.getLocationOnScreen(screenLocation);

        arguments.putInt(ARGUMENT_X, screenLocation[0]);
        arguments.putInt(ARGUMENT_Y, screenLocation[1]);
        arguments.putInt(ARGUMENT_WIDTH, urlView.getWidth());
        arguments.putInt(ARGUMENT_HEIGHT, urlView.getHeight());

        final UrlInputFragment fragment = new UrlInputFragment();
        fragment.setArguments(arguments);

        return fragment;
    }

    private InlineAutocompleteEditText urlView;
    private View clearView;
    private View searchViewContainer;
    private TextView searchView;

    private UrlAutoCompleteFilter urlAutoCompleteFilter;
    private View dismissView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_urlinput, container, false);

        dismissView = view.findViewById(R.id.dismiss);
        dismissView.setOnClickListener(this);

        clearView = view.findViewById(R.id.clear);
        clearView.setOnClickListener(this);

        searchViewContainer = view.findViewById(R.id.search_hint_container);

        searchView = (TextView) view.findViewById(R.id.search_hint);
        searchView.setOnClickListener(this);

        urlAutoCompleteFilter = new UrlAutoCompleteFilter();
        urlAutoCompleteFilter.loadDomainsInBackground(getContext().getApplicationContext());

        urlView = (InlineAutocompleteEditText) view.findViewById(R.id.url_edit);
        urlView.setOnFilterListener(this);
        urlView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Avoid showing keyboard again when returning to the previous page by back key.
                if (hasFocus) {
                    ViewUtils.showKeyboard(urlView);
                }
            }
        });

        urlView.setOnCommitListener(this);

        if (getArguments().containsKey(ARGUMENT_URL)) {
            urlView.setText(getArguments().getString(ARGUMENT_URL));
            clearView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public boolean onBackPressed() {
        dismiss();
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        urlView.requestFocus();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear:
                urlView.setText("");
                urlView.requestFocus();
                break;

            case R.id.search_hint:
                onSearch();
                break;

            case R.id.dismiss:
                dismiss();
                break;

            default:
                throw new IllegalStateException("Unhandled view in onClick()");
        }
    }

    private void dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, FragmentListener.TYPE.DISMISS, true);
        }
    }

    @Override
    public void onCommit() {
        final String input = urlView.getText().toString();
        if (!input.trim().isEmpty()) {
            ViewUtils.hideKeyboard(urlView);

            final boolean isUrl = UrlUtils.isUrl(input);

            final String url = isUrl
                    ? UrlUtils.normalize(input)
                    : UrlUtils.createSearchUrl(getContext(), input);

            openUrl(url);

            TelemetryWrapper.urlBarEvent(isUrl);
        }
    }

    private void onSearch() {
        final String searchUrl = UrlUtils.createSearchUrl(getContext(), urlView.getOriginalText());

        openUrl(searchUrl);

        TelemetryWrapper.searchSelectEvent();
    }

    private void openUrl(String url) {
        final Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, FragmentListener.TYPE.OPEN_URL, url);
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

        urlAutoCompleteFilter.onFilter(searchText, view);

        if (searchText.length() == 0) {
            clearView.setVisibility(View.GONE);
            searchViewContainer.setVisibility(View.GONE);
        } else {
            clearView.setVisibility(View.VISIBLE);

            final String hint = getString(R.string.search_hint, searchText);

            final SpannableString content = new SpannableString(hint);
            content.setSpan(new StyleSpan(Typeface.BOLD), hint.length() - searchText.length(), hint.length(), 0);

            searchView.setText(content);
            searchViewContainer.setVisibility(View.VISIBLE);
        }
    }
}
