/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.mozilla.focus.R;

class SiteViewHolder extends RecyclerView.ViewHolder {

    AppCompatImageView img;
    TextView text;

    public SiteViewHolder(View itemView) {
        super(itemView);
        img = (AppCompatImageView) itemView.findViewById(R.id.content_image);
        text = (TextView) itemView.findViewById(R.id.text);
    }
}
