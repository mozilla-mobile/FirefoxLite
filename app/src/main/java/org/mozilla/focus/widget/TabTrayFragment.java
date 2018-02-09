/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import org.mozilla.focus.R;
import org.mozilla.focus.tabs.Tab;

import java.util.ArrayList;
import java.util.List;

public class TabTrayFragment extends DialogFragment implements TabTrayContract.View {

    public static final String FRAGMENT_TAG = "tab_tray";

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;
    private static final boolean ENABLE_SWIPE_TO_DISMISS = false;

    private static final int TAB_ITEM_MARGIN = 20;

    private TabTrayContract.Presenter presenter;
    private TabTrayContract.Model model;

    private RecyclerView recyclerView;
    private View newTabBtn;
    private View background;

    private TabTrayAdapter adapter = new TabTrayAdapter();

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public static TabTrayFragment newInstance() {
        return new TabTrayFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TabTrayTheme);

        presenter = new TabTrayPresenter(this, model = new TabsModel());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_tray, container, false);
        recyclerView = view.findViewById(R.id.tab_tray);
        newTabBtn = view.findViewById(R.id.new_tab_button);
        background = view.findViewById(R.id.background);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setBottomSheetCallback(behaviorCallback);
        }

        adapter.setTabClickListener(tabClickListener);
        adapter.setDataModel(model);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new TabTrayItemDecoration(TAB_ITEM_MARGIN));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recyclerView);

        setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);

        newTabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                setCollapseHeight(calculateCollapseHeight(view));
                recyclerView.scrollToPosition(model.getCurrentTabPosition());
                return false;
            }
        });
    }

    @Override
    public void tabSwitched(int tabPosition) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        });
    }

    @Override
    public void tabRemoved(int tabPosition) {
        adapter.notifyItemRemoved(tabPosition);
        adapter.notifyItemChanged(model.getCurrentTabPosition());
    }

    @Nullable
    private BottomSheetBehavior getBehavior(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            return null;
        }

        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (behavior == null) {
            return null;
        }

        if (behavior instanceof BottomSheetBehavior) {
            return (BottomSheetBehavior) behavior;
        }
        return null;
    }

    private void setBottomSheetState(@BottomSheetBehavior.State int state) {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setState(state);
        }
    }

    private void setCollapseHeight(int height) {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setPeekHeight(height);
        }
    }

    private int getCollapseHeight() {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            return behavior.getPeekHeight();
        }
        return 0;
    }

    private int calculateCollapseHeight(View rootView) {
        return rootView.getMeasuredHeight() / 2 + newTabBtn.getMeasuredHeight();
    }

    private BottomSheetCallback behaviorCallback = new BottomSheetCallback() {
        private Interpolator interpolator = new AccelerateInterpolator();
        private int collapseHeight = 0;

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            float backgroundAlpha = 1f;
            float btnTranslationY = -1;

            if (slideOffset < 0) {
                if (collapseHeight < 0) {
                    collapseHeight = getCollapseHeight();
                }
                btnTranslationY = collapseHeight * -slideOffset;

                if (ENABLE_BACKGROUND_ALPHA_TRANSITION) {
                    float interpolated = interpolator.getInterpolation(-slideOffset);
                    backgroundAlpha = 1 - interpolated;
                }
            }

            newTabBtn.setTranslationY(btnTranslationY);
            background.setAlpha(backgroundAlpha);
        }
    };

    private TabTrayAdapter.TabClickListener tabClickListener = new TabTrayAdapter.TabClickAdapter() {
        @Override
        public void onItemClick(Tab tab) {
            presenter.tabClicked(tab);
        }

        @Override
        public void onCloseClick(Tab tab) {
            presenter.tabCloseClicked(tab);
        }
    };

    private ItemTouchHelper.Callback touchHelperCallback = new ItemTouchHelper.Callback() {
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int swipeFlag = ItemTouchHelper.START | ItemTouchHelper.END;
            //noinspection ConstantConditions
            return ENABLE_SWIPE_TO_DISMISS ? makeMovementFlags(0, swipeFlag) : 0;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            presenter.tabCloseClicked(model.getTabs().get(viewHolder.getAdapterPosition()));
        }
    };

    public static class TabTrayItemDecoration extends RecyclerView.ItemDecoration {
        private int margin;

        TabTrayItemDecoration(int margin) {
            this.margin = margin;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = margin;
            }
            outRect.left = margin;
            outRect.right = margin;
            outRect.bottom = margin;
        }
    }

//    private static class TabsSessionModel implements TabTrayContract.Model {
//        private WeakReference<TabsSession> weakSession;
//
//        TabsSessionModel(TabTrayFragment fragment) {
//            weakSession = new WeakReference<>(locateTabsSession(fragment));
//        }
//
//        @Override
//        public List<Tab> getTabs() {
//            TabsSession session = weakSession.get();
//            if (session == null) {
//                return new ArrayList<>();
//            }
//            return session.getTabs();
//        }
//
//        @Override
//        public int getTabCount() {
//            TabsSession session = weakSession.get();
//            if (session == null) {
//                return 0;
//            }
//            return session.getTabsCount();
//        }
//
//        @Override
//        public int getCurrentTabPosition() {
//            TabsSession session = weakSession.get();
//            if (session == null) {
//                return -1;
//            }
//            return session.getTabs().indexOf(session.getCurrentTab());
//        }
//
//        @Override
//        public void switchTab(int tabIdx) {
//            TabsSession session = weakSession.get();
//            if (session == null) {
//                return;
//            }
//            session.switchToTab(tabIdx);
//        }
//
//        @Override
//        public void removeTab(Tab tab) {
//            TabsSession session = weakSession.get();
//            if (session == null) {
//                return;
//            }
//            session.removeTab(session.getTabs().indexOf(tab));
//        }
//
//        @Nullable
//        private TabsSession locateTabsSession(TabTrayFragment fragment) {
//            // TODO: Improve how we get TabsSession instance
//            MainActivity activity = (MainActivity) fragment.getActivity();
//            if (activity == null) {
//                return null;
//            }
//
//            FragmentManager fragmentManager = activity.getSupportFragmentManager();
//            Fragment browserFragment = fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
//            if (browserFragment == null) {
//                return null;
//            }
//
//            return ((BrowserFragment) browserFragment).tabsSession;
//        }
//    }

    private static class TabsModel implements TabTrayContract.Model {
        private List<Tab> tabs = new ArrayList<>();
        private int currentTabPosition;

        TabsModel() {
            for (int i = 0; i < 15; i++) {
                Tab tab = new Tab();
                tabs.add(tab);
            }
            currentTabPosition = 10;
        }

        @Override
        public int getTabCount() {
            return tabs.size();
        }

        @Override
        public List<Tab> getTabs() {
            return tabs;
        }

        @Override
        public int getCurrentTabPosition() {
            return currentTabPosition;
        }

        @Override
        public void switchTab(int tabIdx) {
            currentTabPosition = tabIdx;
        }

        @Override
        public void removeTab(Tab tab) {
            int removePos = tabs.indexOf(tab);
            Tab focusedTab = tabs.get(currentTabPosition);

            if (removePos == currentTabPosition) {
                if (removePos > 0) {
                    currentTabPosition = removePos - 1;
                }
                tabs.remove(tabs.indexOf(tab));

            } else {
                tabs.remove(tabs.indexOf(tab));
                currentTabPosition = tabs.indexOf(focusedTab);
            }
        }
    }
}
