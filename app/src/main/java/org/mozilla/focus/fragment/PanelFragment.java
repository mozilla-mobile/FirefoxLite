/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class PanelFragment extends Fragment {

    protected void closePanel() {
        ((ListPanelDialog)getParentFragment()).dismiss();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if( ! (getParentFragment() instanceof ListPanelDialog) ) {
            throw new RuntimeException("PanelFragments needs its parent to be an instance of ListPanelDialog");
        }
    }

    public abstract void tryLoadMore();

}
