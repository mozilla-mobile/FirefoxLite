/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.autocomplete;

import android.content.Context;
import android.text.TextUtils;

import org.mozilla.focus.widget.InlineAutocompleteEditText;

import mozilla.components.browser.domains.DomainAutoCompleteProvider;

public class UrlAutoCompleteFilter implements InlineAutocompleteEditText.OnFilterListener {
    private static final String LOG_TAG = "UrlAutoCompleteFilter";

    private DomainAutoCompleteProvider domainAutoCompleteProvider;

    public UrlAutoCompleteFilter(Context context) {
        domainAutoCompleteProvider = new DomainAutoCompleteProvider();
        domainAutoCompleteProvider.initialize(context, true, false, true);
    }

    @Override
    public void onFilter(final String rawSearchText, InlineAutocompleteEditText view) {
        final String autocomplete = domainAutoCompleteProvider.autocomplete(rawSearchText).getText();
        if (!TextUtils.isEmpty(autocomplete) && view != null) {
            view.onAutocomplete(autocomplete);
        }
    }
}
