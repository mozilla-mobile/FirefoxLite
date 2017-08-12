/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.home.model.Site;
import org.mozilla.focus.widget.FragmentListener;

import java.util.List;

public class HomeFragment extends Fragment implements TopSitesContract.View {

    public static final String FRAGMENT_TAG = "homescreen";

    private TopSitesContract.Presenter presenter;
    private RecyclerView recyclerView;
    private View btnMenu;
    private View fakeInput;
    private SiteItemClickListener clickListener = new SiteItemClickListener();

    public static HomeFragment create() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.presenter = new TopSitesPresenter();
        this.presenter.setView(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_homescreen, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.main_list);
        this.btnMenu = view.findViewById(R.id.btn_menu);
        this.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity parent = getActivity();
                if (parent instanceof FragmentListener) {
                    ((FragmentListener) parent).onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_MENU,
                            null);
                }
            }
        });

        this.fakeInput = view.findViewById(R.id.home_fragment_fake_input);
        this.fakeInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity parent = getActivity();
                if (parent instanceof FragmentListener) {
                    ((FragmentListener) parent).onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_URL_INPUT,
                            null);
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
        this.presenter.populateSites();
    }

    @Override
    public void onStart() {
        super.onStart();
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
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STOPPED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void showSites(@NonNull List<Site> sites) {
        this.recyclerView.setAdapter(
                new TopSiteAdapter(sites,
                        clickListener,
                        clickListener));
    }

    @Override
    public void appendSite(@NonNull Site site) {
        final TopSiteAdapter adapter = (TopSiteAdapter) this.recyclerView.getAdapter();
        adapter.addSite(adapter.getItemCount(), site);
    }

    @Override
    public void removeSite(@NonNull Site site) {
        ((TopSiteAdapter) this.recyclerView.getAdapter()).removeSite(site);
    }

    @Override
    public void onSiteChanged(@NonNull Site site) {
        throw new NoSuchMethodError("Not implement yet");
    }

    public void setPresenter(TopSitesContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void toggleFakeUrlInput(boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        this.fakeInput.setVisibility(visibility);
    }

    private class SiteItemClickListener implements View.OnClickListener, View.OnLongClickListener {

        @Override
        public void onClick(View v) {
            final Site site = (Site) v.getTag();
            final Activity parent = getActivity();
            if ((site != null) && (parent instanceof FragmentListener)) {
                ((FragmentListener) parent).onNotified(HomeFragment.this,
                        FragmentListener.TYPE.OPEN_URL,
                        site.getUrl());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            final Site site = (Site) v.getTag();

            if (site == null) {
                Log.w(FRAGMENT_TAG, "Long-clicked item has no site info");
                return false;
            }

            v.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu,
                                                View v,
                                                ContextMenu.ContextMenuInfo menuInfo) {

                    final MenuInflater inflater = new MenuInflater(getContext());
                    inflater.inflate(R.menu.menu_top_site_item, menu);
                    menu.findItem(R.id.remove)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    HomeFragment.this.presenter.removeSite(site);
                                    return true;
                                }
                            });
                }
            });
            v.showContextMenu();

            return true;
        }
    }
}
