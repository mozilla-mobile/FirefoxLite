/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.util.DiffUtil;
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
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import com.bumptech.glide.Glide;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.rocket.content.HomeFragmentViewState;
import org.mozilla.rocket.nightmode.themed.ThemedImageView;
import org.mozilla.rocket.nightmode.themed.ThemedRecyclerView;
import org.mozilla.rocket.nightmode.themed.ThemedRelativeLayout;
import org.mozilla.rocket.nightmode.themed.ThemedView;
import org.mozilla.rocket.privately.PrivateMode;
import org.mozilla.rocket.privately.PrivateModeActivity;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabsSessionProvider;

import java.util.List;

public class TabTrayFragment extends DialogFragment implements TabTrayContract.View,
        View.OnClickListener, TabTrayAdapter.TabClickListener {

    public static final String FRAGMENT_TAG = "tab_tray";

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;
    private static final boolean ENABLE_SWIPE_TO_DISMISS = true;

    private static final float OVERLAY_ALPHA_FULL_EXPANDED = 0.50f;

    private TabTrayContract.Presenter presenter;

    private ThemedRelativeLayout newTabBtn;
    private View logoMan;
    private View closeTabsBtn;
    private View privateModeBtn;
    private View privateModeBadge;

    private AlertDialog closeTabsDialog;

    private View backgroundView;
    private Drawable backgroundDrawable;
    private Drawable backgroundOverlay;

    private ThemedRecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private boolean playEnterAnimation = true;

    private TabTrayAdapter adapter;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private SlideAnimationCoordinator slideCoordinator = new SlideAnimationCoordinator(this);

    private Runnable dismissRunnable = this::dismissAllowingStateLoss;

    private TabTrayViewModel tabTrayViewModel = null;

    private ThemedImageView imgPrivateBrowsing, imgNewTab;
    private ThemedView bottomDivider;

    public static TabTrayFragment newInstance() {
        return new TabTrayFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TabTrayTheme);

        adapter = new TabTrayAdapter(Glide.with(this));

        SessionManager sessionManager = TabsSessionProvider.getOrThrow(getActivity());
        presenter = new TabTrayPresenter(this, new TabsSessionModel(sessionManager));
    }

    @Override
    public void onStart() {
        if (playEnterAnimation) {
            playEnterAnimation = false;
            setDialogAnimation(R.style.TabTrayDialogEnterExit);

        } else {
            setDialogAnimation(R.style.TabTrayDialogExit);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (closeTabsDialog != null && closeTabsDialog.isShowing()) {
            closeTabsDialog.dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tab_tray, container, false);
        recyclerView = view.findViewById(R.id.tab_tray);
        newTabBtn = view.findViewById(R.id.new_tab_button);
        closeTabsBtn = view.findViewById(R.id.close_all_tabs_btn);
        privateModeBtn = view.findViewById(R.id.btn_private_browsing);
        privateModeBadge = view.findViewById(R.id.badge_in_private_mode);
        tabTrayViewModel = ViewModelProviders.of(this).get(TabTrayViewModel.class);
        tabTrayViewModel.hasPrivateTab().observe(this, hasPrivateTab -> {
            // Update the UI, in this case, a TextView.
            if (privateModeBadge != null) {
                privateModeBadge.setVisibility(hasPrivateTab ? View.VISIBLE : View.INVISIBLE);
            }
        });
        privateModeBtn.setVisibility(PrivateMode.isEnable(getContext()) ? View.VISIBLE : View.INVISIBLE);
        backgroundView = view.findViewById(R.id.root_layout);
        logoMan = backgroundView.findViewById(R.id.logo_man);
        imgPrivateBrowsing = view.findViewById(R.id.img_private_browsing);
        imgNewTab = view.findViewById(R.id.plus_sign);
        bottomDivider = view.findViewById(R.id.bottom_divider);
        view.findViewById(R.id.star_background).setVisibility(Settings.getInstance(getContext()).isNightModeEnable() ? View.VISIBLE : View.GONE);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setNightModeEnabled(Settings.getInstance(view.getContext()).isNightModeEnable());

        initWindowBackground(view.getContext());

        setupBottomSheetCallback();

        prepareExpandAnimation();

        initRecyclerView();

        newTabBtn.setOnClickListener(this);
        closeTabsBtn.setOnClickListener(this);
        privateModeBtn.setOnClickListener(this);
        setupTapBackgroundToExpand();

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                startExpandAnimation();
                presenter.viewReady();
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        tabTrayViewModel.hasPrivateTab().setValue(PrivateMode.hasPrivateSession(getContext()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_tab_button:
                onNewTabClicked();
                break;

            case R.id.close_all_tabs_btn:
                onCloseAllTabsClicked();
                break;

            case R.id.btn_private_browsing:
                TelemetryWrapper.privateModeTray();
                startActivity(new Intent(getContext(), PrivateModeActivity.class));
                getActivity().overridePendingTransition(R.anim.pb_enter, R.anim.pb_exit);
                break;

            default:
                break;
        }
    }

    @Override
    public void onTabClick(int tabPosition) {
        HomeFragmentViewState.reset();
        presenter.tabClicked(tabPosition);
        TelemetryWrapper.clickTabFromTabTray();
    }

    @Override
    public void onTabCloseClick(int tabPosition) {
        HomeFragmentViewState.reset();
        presenter.tabCloseClicked(tabPosition);
        TelemetryWrapper.closeTabFromTabTray();
    }

    @Override
    public void initData(List<Session> newTabs, Session newFocusedTab) {
        adapter.setData(newTabs);
        adapter.setFocusedTab(newFocusedTab);
    }

    @Override
    public void refreshData(final List<Session> newTabs, final Session newFocusedTab) {
        final List<Session> oldTabs = adapter.getData();
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldTabs.size();
            }

            @Override
            public int getNewListSize() {
                return newTabs.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return newTabs.get(newItemPosition).getId().equals(oldTabs.get(oldItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return true;
            }
        }, false).dispatchUpdatesTo(adapter);
        adapter.setData(newTabs);

        waitItemAnimation(() -> {
            Session oldFocused = adapter.getFocusedTab();
            List<Session> oldTabs1 = adapter.getData();
            int oldFocusedPosition = oldTabs1.indexOf(oldFocused);
            adapter.notifyItemChanged(oldFocusedPosition);

            adapter.setFocusedTab(newFocusedTab);
            int newFocusedPosition = oldTabs1.indexOf(newFocusedTab);
            adapter.notifyItemChanged(newFocusedPosition);
        });
    }

    @Override
    public void refreshTabData(Session tab) {
        List<Session> tabs = adapter.getData();
        int position = tabs.indexOf(tab);
        if (position >= 0 && position < tabs.size()) {
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public void showFocusedTab(int tabPosition) {
        layoutManager.scrollToPositionWithOffset(tabPosition,
                recyclerView.getMeasuredHeight() / 2);
    }

    @Override
    public void tabSwitched(int tabPosition) {
        ScreenNavigator.get(getContext()).raiseBrowserScreen(false);
        postOnNextFrame(dismissRunnable);
    }

    @Override
    public void closeTabTray() {
        postOnNextFrame(dismissRunnable);
    }

    @Override
    public void navigateToHome() {
        ScreenNavigator.get(getContext()).popToHomeScreen(false);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (presenter != null) {
            presenter.tabTrayClosed();
        }
    }

    private void setupBottomSheetCallback() {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior == null) {
            return;
        }

        behavior.setBottomSheetCallback(new BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                slideCoordinator.onSlide(slideOffset);
            }
        });
    }

    private void initRecyclerView() {
        initRecyclerViewStyle(recyclerView);
        setupSwipeToDismiss(recyclerView);

        adapter.setTabClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeToDismiss(RecyclerView recyclerView) {
        int swipeFlag = ENABLE_SWIPE_TO_DISMISS ? ItemTouchHelper.START | ItemTouchHelper.END : 0;
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, swipeFlag) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                HomeFragmentViewState.reset();
                presenter.tabCloseClicked(viewHolder.getAdapterPosition());
                TelemetryWrapper.swipeTabFromTabTray();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    float alpha = 1f - (Math.abs(dX) / (recyclerView.getWidth() / 2f));
                    viewHolder.itemView.setAlpha(alpha);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void prepareExpandAnimation() {
        setBottomSheetState(BottomSheetBehavior.STATE_HIDDEN);

        // update logo-man and background alpha state
        slideCoordinator.onSlide(-1);
        logoMan.setVisibility(View.INVISIBLE);
    }

    private void startExpandAnimation() {
        List<Session> tabs = adapter.getData();
        int focusedPosition = tabs.indexOf(adapter.getFocusedTab());
        final boolean shouldExpand = isPositionVisibleWhenCollapse(focusedPosition);
        uiHandler.postDelayed(() -> {
            if (shouldExpand) {
                setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED);
                logoMan.setVisibility(View.VISIBLE);
                setIntercept(false);
            } else {
                setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                setIntercept(true);
            }
        }, getResources().getInteger(R.integer.tab_tray_transition_time));
    }

    private boolean isPositionVisibleWhenCollapse(int focusedPosition) {
        Resources res = getResources();
        int visiblePanelHeight = res.getDimensionPixelSize(R.dimen.tab_tray_peekHeight) -
                res.getDimensionPixelSize(R.dimen.tab_tray_new_tab_btn_height);
        int itemHeightWithDivider = res.getDimensionPixelSize(R.dimen.tab_tray_item_height) +
                res.getDimensionPixelSize(R.dimen.tab_tray_item_space);
        final int visibleItemCount = visiblePanelHeight / itemHeightWithDivider;

        return focusedPosition < visibleItemCount;
    }

    private void waitItemAnimation(final Runnable onAnimationEnd) {
        uiHandler.post(() -> {
            RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
            if (animator == null) {
                return;
            }

            animator.isRunning(() -> uiHandler.post(onAnimationEnd));
        });
    }

    @Nullable
    private InterceptBehavior getBehavior(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof CoordinatorLayout.LayoutParams)) {
            return null;
        }

        CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params).getBehavior();
        if (behavior == null) {
            return null;
        }

        if (behavior instanceof InterceptBehavior) {
            return (InterceptBehavior) behavior;
        }
        return null;
    }

    private void setBottomSheetState(@BottomSheetBehavior.State int state) {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setState(state);
        }
    }

    private int getBottomSheetState() {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            return behavior.getState();
        }
        return -1;
    }

    private int getCollapseHeight() {
        BottomSheetBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            return behavior.getPeekHeight();
        }
        return 0;
    }

    private void setIntercept(boolean intercept) {
        InterceptBehavior behavior = getBehavior(recyclerView);
        if (behavior != null) {
            behavior.setIntercept(intercept);
        }
    }

    private void initRecyclerViewStyle(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));

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

        backgroundView.setOnTouchListener((v, event) -> {
            boolean result = detector.onTouchEvent(event);
            if (result) {
                v.performClick();
            }
            return result;
        });
    }

    private void onNewTabClicked() {
        HomeFragmentViewState.reset();
        ScreenNavigator.get(getContext()).addHomeScreen(false);
        TelemetryWrapper.clickAddTabTray();
        postOnNextFrame(dismissRunnable);
    }

    private void onCloseAllTabsClicked() {
        if (closeTabsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            closeTabsDialog = builder.setMessage(R.string.tab_tray_close_tabs_dialog_msg)
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        presenter.closeAllTabs();
                        TelemetryWrapper.closeAllTabFromTabTray();
                    })
                    .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            closeTabsDialog.show();
        }
    }

    private void initWindowBackground(Context context) {
        Drawable drawable = context.getDrawable(R.drawable.tab_tray_background);
        if (drawable == null) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException("fail to resolve background drawable");
            }
            return;
        }

        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            if (Settings.getInstance(getContext()).isNightModeEnable()) {
                backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.gradient_background_night);
                // set alpha = 0 to let this layer invisible
                layerDrawable.findDrawableByLayerId(R.id.gradient_background).setAlpha(0);
            } else {
                backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.gradient_background);
                layerDrawable.findDrawableByLayerId(R.id.gradient_background_night).setAlpha(0);
            }
            backgroundOverlay = layerDrawable.findDrawableByLayerId(R.id.background_overlay);
            int alpha = validateBackgroundAlpha(0xff);
            backgroundDrawable.setAlpha(alpha);
            backgroundOverlay.setAlpha(getBottomSheetState() == BottomSheetBehavior.STATE_COLLAPSED ? 0 : (int) (alpha * OVERLAY_ALPHA_FULL_EXPANDED));

        } else {
            backgroundDrawable = drawable;
        }

        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(drawable);
    }

    private int validateBackgroundAlpha(int alpha) {
        return Math.max(Math.min(alpha, 0xfe), 0x01);
    }

    private void setDialogAnimation(@StyleRes int resId) {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = resId;
            updateWindowAttrs(window);
        }
    }

    private void updateWindowAttrs(@NonNull Window window) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return;
        }

        View decor = window.getDecorView();
        if (decor.isAttachedToWindow()) {
            manager.updateViewLayout(decor, window.getAttributes());
        }
    }

    private void onTranslateToHidden(float translationY) {
        newTabBtn.setTranslationY(translationY);
        logoMan.setTranslationY(translationY);
    }

    private void updateWindowBackground(float backgroundAlpha) {
        backgroundView.setAlpha(backgroundAlpha);

        if (backgroundDrawable != null) {
            backgroundDrawable.setAlpha(validateBackgroundAlpha((int) (backgroundAlpha * 0xff)));
        }
    }

    private void updateWindowOverlay(float overlayAlpha) {
        if (backgroundOverlay != null) {
            backgroundOverlay.setAlpha(validateBackgroundAlpha((int) (overlayAlpha * 0xff)));
        }
    }

    private void onFullyExpanded() {
        if (logoMan.getVisibility() != View.VISIBLE) {
            // We don't want to show logo-man during fully expand animation (too complex visually).
            // In this case, we hide logo-man at first, and make sure it become visible after tab
            // tray is fully expanded (slideOffset >= 1). See prepareExpandAnimation()
            logoMan.setVisibility(View.VISIBLE);
        }
        setIntercept(false);
    }

    private void postOnNextFrame(final Runnable runnable) {
        uiHandler.post(() -> uiHandler.post(runnable));
    }

    private static class SlideAnimationCoordinator {
        private Interpolator backgroundInterpolator = new AccelerateInterpolator();
        private Interpolator overlayInterpolator = new AccelerateDecelerateInterpolator();
        private int collapseHeight = -1;

        private float translationY = Integer.MIN_VALUE;
        private float backgroundAlpha = -1;
        private float overlayAlpha = -1;

        private TabTrayFragment fragment;

        SlideAnimationCoordinator(TabTrayFragment fragment) {
            this.fragment = fragment;
        }

        private void onSlide(float slideOffset) {
            float backgroundAlpha = 1f;
            float overlayAlpha = 0f;

            float translationY = 0;

            if (slideOffset < 0) {
                if (collapseHeight < 0) {
                    collapseHeight = fragment.getCollapseHeight();
                }
                translationY = collapseHeight * -slideOffset;

                if (ENABLE_BACKGROUND_ALPHA_TRANSITION) {
                    float interpolated = backgroundInterpolator.getInterpolation(-slideOffset);
                    backgroundAlpha = Math.max(0, 1 - interpolated);
                }
            } else {
                float interpolated = overlayInterpolator.getInterpolation(1 - slideOffset);
                overlayAlpha = -(interpolated * OVERLAY_ALPHA_FULL_EXPANDED) + OVERLAY_ALPHA_FULL_EXPANDED;
            }

            if (slideOffset >= 1) {
                fragment.onFullyExpanded();
            }

            if (Float.compare(this.translationY, translationY) != 0) {
                this.translationY = translationY;
                fragment.onTranslateToHidden(translationY);
            }

            if (Float.compare(this.backgroundAlpha, backgroundAlpha) != 0) {
                this.backgroundAlpha = backgroundAlpha;
                fragment.updateWindowBackground(backgroundAlpha);
            }

            if (Float.compare(this.overlayAlpha, overlayAlpha) != 0) {
                this.overlayAlpha = overlayAlpha;
                fragment.updateWindowOverlay(overlayAlpha);
            }
        }
    }

    private void setNightModeEnabled(boolean enable) {
        newTabBtn.setNightMode(enable);
        imgPrivateBrowsing.setNightMode(enable);
        imgNewTab.setNightMode(enable);
        bottomDivider.setNightMode(enable);
        recyclerView.setNightMode(enable);

        ViewUtils.updateStatusBarStyle(!enable, getDialog().getWindow());
    }
}
