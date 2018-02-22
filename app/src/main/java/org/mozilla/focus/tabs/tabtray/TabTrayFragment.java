/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.widget.FragmentListener;

import java.util.List;

public class TabTrayFragment extends DialogFragment implements TabTrayContract.View,
        View.OnClickListener {

    public static final String FRAGMENT_TAG = "tab_tray";

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;
    private static final boolean ENABLE_SWIPE_TO_DISMISS = false;

    private TabTrayContract.Presenter presenter;

    private View newTabBtn;
    private View logoMan;

    private View backgroundView;
    private Drawable backgroundDrawable;

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
        backgroundView = view.findViewById(R.id.root_layout);
        logoMan = backgroundView.findViewById(R.id.logo_man);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initBackground(view.getContext());

        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setBottomSheetCallback(behaviorCallback);
        }

        adapter.setTabClickListener(tabClickListener);
        recyclerView.setAdapter(adapter);

        initRecyclerViewStyle(recyclerView);
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recyclerView);

        setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);

        newTabBtn.setOnClickListener(this);
        setupTapBackgroundToExpand();

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                presenter.viewReady();
                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_tab_button:
                onNewTabClicked();
                dismiss();
                break;

            default:
                break;
        }
    }

    @Override
    public void updateData(List<Tab> tabs) {
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
    public void tabRemoved(int removePos, int focusPos, int modifiedFocusPos, int nextFocusPos) {
        adapter.notifyItemRemoved(removePos);
        adapter.notifyItemChanged(modifiedFocusPos);
        adapter.setFocusedTab(nextFocusPos);
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

    private int getCollapseHeight() {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            return behavior.getPeekHeight();
        }
        return 0;
    }

    private void initRecyclerViewStyle(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));

        Context context = getContext();
        recyclerView.addItemDecoration(new ItemSpaceDecoration(context));
        recyclerView.addItemDecoration(new TabTrayPaddingDecoration(context, this));

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    private void setupTapBackgroundToExpand() {
        final GestureDetectorCompat detector = new GestureDetectorCompat(getContext(),
                new SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
        });

        backgroundView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = detector.onTouchEvent(event);
                if (result) {
                    v.performClick();
                }
                return result;
            }
        });
    }

    private void onNewTabClicked() {
        Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this,
                    FragmentListener.TYPE.SHOW_HOME, null);
        }
    }

    private void initBackground(Context context) {
        backgroundDrawable = context.getDrawable(R.drawable.tab_tray_background);
        if (backgroundDrawable == null) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("fail to resolve background drawable");
            }
            return;
        }
        int validAlpha = validateBackgroundAlpha(0xff);
        backgroundDrawable.setAlpha(validAlpha);

        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(backgroundDrawable);
    }

    private void setBackgroundAlpha(float alpha) {
        if (backgroundDrawable == null) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("initBackground() should be called first");
            }
            return;
        }

        int validAlpha = validateBackgroundAlpha((int) (alpha * 0xff));
        backgroundDrawable.setAlpha(validAlpha);
    }

    private int validateBackgroundAlpha(int alpha) {
        return Math.max(Math.min(alpha, 0xfe), 0x01);
    }

    private BottomSheetCallback behaviorCallback = new BottomSheetCallback() {
        private Interpolator interpolator = new AccelerateInterpolator();
        private int collapseHeight = -1;

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            float backgroundAlpha = 1f;
            float translationY = 0;

            if (slideOffset < 0) {
                if (collapseHeight < 0) {
                    collapseHeight = getCollapseHeight();
                }
                translationY = collapseHeight * -slideOffset;

                if (ENABLE_BACKGROUND_ALPHA_TRANSITION) {
                    float interpolated = interpolator.getInterpolation(-slideOffset);
                    backgroundAlpha = 1 - interpolated;
                }
            }

            newTabBtn.setTranslationY(translationY);
            logoMan.setTranslationY(translationY);

            backgroundAlpha = backgroundAlpha < 0 ? 0 : backgroundAlpha;
            backgroundView.setAlpha(backgroundAlpha);

            setBackgroundAlpha(backgroundAlpha);
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

    public static class ItemSpaceDecoration extends RecyclerView.ItemDecoration {
        private int margin;

        ItemSpaceDecoration(Context context) {
            this.margin = context.getResources().getDimensionPixelSize(R.dimen.tab_tray_item_space);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int itemPosition = parent.getChildAdapterPosition(view);
            outRect.top = itemPosition == 0 ? 0 : margin;
        }
    }

    public static class TabTrayPaddingDecoration extends RecyclerView.ItemDecoration {
        private TabTrayFragment fragment;
        private int padding;

        TabTrayPaddingDecoration(Context context, TabTrayFragment fragment) {
            this.fragment = fragment;
            this.padding = context.getResources().getDimensionPixelSize(R.dimen.tab_tray_padding);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int itemPosition = parent.getChildAdapterPosition(view);
            outRect.left = outRect.right = padding;

            if (itemPosition == 0) {
                outRect.top = padding;
            } else if (itemPosition == fragment.adapter.getItemCount() - 1) {
                outRect.bottom = fragment.newTabBtn.getMeasuredHeight() + padding;
            }
        }
    }

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
