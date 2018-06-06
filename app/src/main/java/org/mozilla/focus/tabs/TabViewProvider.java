/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs;

/**
 * A class to create TabView instance.
 */

public interface TabViewProvider {
    int ENGINE_WEBKIT = 0;
    int ENGINE_GECKO = 1;

    TabView create();

    int getEngineType();
}
