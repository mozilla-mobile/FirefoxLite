/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.app.Activity;
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
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionHost;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TabTrayFragment extends DialogFragment implements TabTrayContract.View {

    public static final String FRAGMENT_TAG = "tab_tray";

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;
    private static final boolean ENABLE_SWIPE_TO_DISMISS = false;

    private static final int TAB_ITEM_MARGIN = 20;

    private TabTrayContract.Presenter presenter;

    private View newTabBtn;
    private View background;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private TabTrayAdapter adapter = new TabTrayAdapter();

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public static TabTrayFragment newInstance() {
        return new TabTrayFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TabTrayTheme);

        presenter = new TabTrayPresenter(this, new TabsSessionModel(this));
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

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(getContext(),
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
                presenter.viewReady();
                return false;
            }
        });
    }

    @Override
    public void showTabs(List<Tab> tabs) {
        adapter.setData(tabs);
    }

    @Override
    public void setFocusedTab(int tabPosition) {
        adapter.setFocusedTab(tabPosition);
    }

    @Override
    public void showFocusedTab(int tabPosition) {
        layoutManager.scrollToPositionWithOffset(tabPosition,
                recyclerView.getMeasuredHeight() / 2);
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
    public void tabRemoved(int tabPosition, final int currentFocusPosition,  final int nextFocusPosition) {
        if (tabPosition == currentFocusPosition) {
            adapter.setFocusedTab(-1);
        } else {
            adapter.setFocusedTab(nextFocusPosition);
        }

        adapter.notifyItemRemoved(tabPosition);
        adapter.notifyItemChanged(nextFocusPosition);

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
                if (animator == null) {
                    return;
                }

                animator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        adapter.setFocusedTab(nextFocusPosition);
                    }
                });
            }
        });
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
        public void onItemClick(int tabPosition) {
            presenter.tabClicked(tabPosition);
        }

        @Override
        public void onCloseClick(int tabPosition) {
            presenter.tabCloseClicked(tabPosition);
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
            presenter.tabCloseClicked(viewHolder.getAdapterPosition());
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

    private static class TabsSessionModel implements TabTrayContract.Model {
        private WeakReference<TabsSession> weakSession;

        TabsSessionModel(TabTrayFragment fragment) {
            weakSession = new WeakReference<>(locateTabsSession(fragment));
        }

        @Override
        public List<Tab> getTabs() {
            TabsSession session = weakSession.get();
            if (session == null) {
                return new ArrayList<>();
            }
            return session.getTabs();
        }

        @Override
        public int getCurrentTabPosition() {
            TabsSession session = weakSession.get();
            if (session == null) {
                return -1;
            }
            return session.getTabs().indexOf(session.getCurrentTab());
        }

        @Override
        public void switchTab(int tabIdx) {
            TabsSession session = weakSession.get();
            if (session == null) {
                return;
            }
            session.switchToTab(tabIdx);
        }

        @Override
        public void removeTab(int tabPosition) {
            TabsSession session = weakSession.get();
            if (session == null) {
                return;
            }
            session.removeTab(tabPosition);
        }

        @Nullable
        private TabsSession locateTabsSession(TabTrayFragment fragment) {
            Activity activity = fragment.getActivity();
            if (activity instanceof TabsSessionHost) {
                return ((TabsSessionHost) activity).getTabsSession();
            }
            return null;
        }
    }
//
//    private static class TabsModel implements TabTrayContract.Model {
//        private List<Tab> tabs = new ArrayList<>();
//        private int currentTabPosition;
//
//        TabsModel() {
//            for (int i = 0; i < 15; i++) {
//                Tab tab = new Tab();
//                tabs.add(tab);
//            }
//            currentTabPosition = 8;
//        }
//
//        @Override
//        public List<Tab> getTabs() {
//            return tabs;
//        }
//
//        @Override
//        public int getCurrentTabPosition() {
//            return currentTabPosition;
//        }
//
//        @Override
//        public void switchTab(int tabIdx) {
//            currentTabPosition = tabIdx;
//        }
//
//        @Override
//        public void removeTab(int tabPosition) {
//            Tab focusedTab = tabs.get(currentTabPosition);
//
//            if (tabPosition == currentTabPosition) {
//                if (tabPosition > 0) {
//                    currentTabPosition = tabPosition - 1;
//                }
//                tabs.remove(tabPosition);
//
//            } else {
//                tabs.remove(tabPosition);
//                currentTabPosition = tabs.indexOf(focusedTab);
//            }
//        }
//    }
}
