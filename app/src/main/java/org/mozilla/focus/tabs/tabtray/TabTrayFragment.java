/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
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
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.bumptech.glide.Glide;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.tabs.Tab;
import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabsChromeListener;
import org.mozilla.focus.tabs.TabsSession;
import org.mozilla.focus.tabs.TabsSessionProvider;
import org.mozilla.focus.tabs.TabsViewListener;
import org.mozilla.focus.widget.FragmentListener;

import java.util.List;

public class TabTrayFragment extends DialogFragment implements TabTrayContract.View,
        View.OnClickListener, TabTrayAdapter.TabClickListener {

    public static final String FRAGMENT_TAG = "tab_tray";

    private static final boolean ENABLE_BACKGROUND_ALPHA_TRANSITION = true;
    private static final boolean ENABLE_SWIPE_TO_DISMISS = false;

    private static final float OVERLAY_ALPHA_FULL_EXPANDED = 0.50f;

    private TabTrayContract.Presenter presenter;
    private TabsSession tabsSession;

    private View newTabBtn;
    private View logoMan;

    private View backgroundView;
    private Drawable backgroundDrawable;
    private Drawable backgroundOverlay;

    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private boolean playEnterAnimation = true;

    private TabTrayAdapter adapter;
    private OnTabModelChangedListener onTabModelChangedListener;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private SlideAnimationCoordinator slideCoordinator = new SlideAnimationCoordinator(this);

    private Runnable dismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };

    public static TabTrayFragment newInstance() {
        return new TabTrayFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.TabTrayTheme);

        adapter = new TabTrayAdapter(Glide.with(this));

        tabsSession = TabsSessionProvider.getOrThrow(getActivity());
        presenter = new TabTrayPresenter(this, new TabsSessionModel(tabsSession));
    }

    @Override
    public void onStart() {
        if (playEnterAnimation) {
            playEnterAnimation = false;
            setDialogAnimation(R.style.TabTrayDialogEnterExit);

        } else {
            setDialogAnimation(R.style.TabTrayDialogExit);
        }

        if (onTabModelChangedListener == null) {
            onTabModelChangedListener = new OnTabModelChangedListener() {
                @Override
                void onTabModelChanged(Tab tab) {
                    adapter.notifyItemChanged(adapter.getItemPosition(tab));
                }
            };
        }
        tabsSession.addTabsViewListener(onTabModelChangedListener);
        tabsSession.addTabsChromeListener(onTabModelChangedListener);
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        tabsSession.removeTabsViewListener(onTabModelChangedListener);
        tabsSession.removeTabsChromeListener(onTabModelChangedListener);
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

        initWindowBackground(view.getContext());

        setupBottomSheetCallback();

        final Runnable expandRunnable = prepareExpandAnimation();

        initRecyclerView();

        newTabBtn.setOnClickListener(this);
        setupTapBackgroundToExpand();

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                uiHandler.postDelayed(expandRunnable, getResources().getInteger(
                        R.integer.tab_tray_transition_time));
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
                break;

            default:
                break;
        }
    }

    @Override
    public void onTabClick(int tabPosition) {
        presenter.tabClicked(tabPosition);
    }

    @Override
    public void onTabCloseClick(int tabPosition) {
        presenter.tabCloseClicked(tabPosition);
    }

    @Override
    public void updateData(List<Tab> tabs) {
        adapter.setData(tabs);

        if (tabs.isEmpty()) {
            notifyFragmentListener(FragmentListener.TYPE.SHOW_HOME,  new boolean[] {false, false});
            postOnNextFrame(dismissRunnable);
        }
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
        notifyFragmentListener(FragmentListener.TYPE.DISMISS_HOME, null);
        postOnNextFrame(dismissRunnable);
    }

    @Override
    public void tabRemoved(int removePos, int focusPos, int modifiedFocusPos, final int nextFocusPos) {
        animateItemRemove(removePos, new Runnable() {
            @Override
            public void run() {
                adapter.setFocusedTab(nextFocusPos);
                adapter.notifyItemChanged(nextFocusPos);
            }
        });
    }

    private void animateItemRemove(int removePos, final Runnable onAnimationEndCallback) {
        adapter.notifyItemRemoved(removePos);
        if (removePos != 0) {
            adapter.notifyItemChanged(removePos - 1);
        }

        final Runnable monitorAnimationEnd = new Runnable() {
            @Override
            public void run() {
                RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
                if (animator == null) {
                    return;
                }

                animator.isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        uiHandler.post(onAnimationEndCallback);
                    }
                });
            }
        } ;

        // remove animation will start in next frame
        uiHandler.post(monitorAnimationEnd);
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
                    dismiss();
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

            }
        }).attachToRecyclerView(recyclerView);
    }

    private Runnable prepareExpandAnimation() {
        setBottomSheetState(BottomSheetBehavior.STATE_HIDDEN);

        // update logo-man and background alpha state
        slideCoordinator.onSlide(-1);
        logoMan.setVisibility(View.INVISIBLE);

        return new Runnable() {
            @Override
            public void run() {
                if (isPositionVisibleWhenCollapse(adapter.getFocusedTabPosition())) {
                    setBottomSheetState(BottomSheetBehavior.STATE_COLLAPSED);
                    logoMan.setVisibility(View.VISIBLE);
                    setIntercept(false);
                } else {
                    setBottomSheetState(BottomSheetBehavior.STATE_EXPANDED);
                    setIntercept(true);
                }
            }
        };
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
        notifyFragmentListener(FragmentListener.TYPE.SHOW_HOME,  new boolean[] {true, true});
        postOnNextFrame(dismissRunnable);
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
            backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.gradient_background);
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

    @SuppressWarnings("SameParameterValue")
    private void notifyFragmentListener(@NonNull FragmentListener.TYPE type, @Nullable Object payload) {
        Activity activity = getActivity();
        if (activity instanceof FragmentListener) {
            ((FragmentListener) activity).onNotified(this, type, payload);
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
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                uiHandler.post(runnable);
            }
        });
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

    private static abstract class OnTabModelChangedListener implements TabsViewListener,
            TabsChromeListener {
        @Override
        public void onTabStarted(@NonNull Tab tab) {
        }

        @Override
        public void onTabFinished(@NonNull Tab tab, boolean isSecure) {
        }

        @Override
        public void onURLChanged(@NonNull Tab tab, String url) {
            onTabModelChanged(tab);
        }

        @Override
        public boolean handleExternalUrl(String url) {
            return false;
        }

        @Override
        public void updateFailingUrl(@NonNull Tab tab, String url, boolean updateFromError) {
            onTabModelChanged(tab);
        }

        @Override
        public void onProgressChanged(@NonNull Tab tab, int progress) {
        }

        @Override
        public void onReceivedTitle(@NonNull Tab tab, String title) {
            onTabModelChanged(tab);
        }

        @Override
        public void onReceivedIcon(@NonNull Tab tab, Bitmap icon) {
            onTabModelChanged(tab);
        }

        @Override
        public void onFocusChanged(@Nullable Tab tab, @Factor int factor) {
        }

        @Override
        public void onTabAdded(@NonNull Tab tab, @Nullable Bundle arguments) {
        }

        @Override
        public void onTabCountChanged(int count) {
        }

        @Override
        public void onLongPress(@NonNull Tab tab, TabView.HitTarget hitTarget) {
        }

        @Override
        public void onEnterFullScreen(@NonNull Tab tab, @NonNull TabView.FullscreenCallback callback, @Nullable View fullscreenContent) {
        }

        @Override
        public void onExitFullScreen(@NonNull Tab tab) {
        }

        @Override
        public boolean onShowFileChooser(@NonNull Tab tab, WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            return false;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull Tab tab, String origin, GeolocationPermissions.Callback callback) {

        }

        abstract void onTabModelChanged(Tab tab);
    }
}
