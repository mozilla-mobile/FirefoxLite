/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.tabs.tabtray

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import androidx.annotation.StyleRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_tab_tray.badge_in_private_mode
import kotlinx.android.synthetic.main.fragment_tab_tray.bottom_divider
import kotlinx.android.synthetic.main.fragment_tab_tray.close_all_tabs_btn
import kotlinx.android.synthetic.main.fragment_tab_tray.logo_man
import kotlinx.android.synthetic.main.fragment_tab_tray.new_tab_button
import kotlinx.android.synthetic.main.fragment_tab_tray.plus_sign
import kotlinx.android.synthetic.main.fragment_tab_tray.private_browsing_btn
import kotlinx.android.synthetic.main.fragment_tab_tray.private_browsing_img
import kotlinx.android.synthetic.main.fragment_tab_tray.root_layout
import kotlinx.android.synthetic.main.fragment_tab_tray.star_background
import kotlinx.android.synthetic.main.fragment_tab_tray.tab_tray_recycler_view
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.tabs.tabtray.TabTrayAdapter.ShoppingSearchViewHolder
import org.mozilla.focus.tabs.tabtray.TabTrayAdapter.TabViewHolder
import org.mozilla.focus.telemetry.TelemetryWrapper.clickAddTabTray
import org.mozilla.focus.telemetry.TelemetryWrapper.clickTabFromTabTray
import org.mozilla.focus.telemetry.TelemetryWrapper.closeAllTabFromTabTray
import org.mozilla.focus.telemetry.TelemetryWrapper.closeTabFromTabTray
import org.mozilla.focus.telemetry.TelemetryWrapper.privateModeTray
import org.mozilla.focus.telemetry.TelemetryWrapper.swipeTabFromTabTray
import org.mozilla.focus.utils.Settings
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.privately.PrivateMode.Companion.getInstance
import org.mozilla.rocket.privately.PrivateModeActivity
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity.Companion.getStartIntent
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class TabTrayFragment : DialogFragment(), TabTrayContract.View, View.OnClickListener, TabTrayAdapter.TabClickListener {
    @Inject
    lateinit var tabTrayViewModelCreator: Lazy<TabTrayViewModel>

    @Inject
    lateinit var homeViewModelCreator: Lazy<HomeViewModel>

    private lateinit var presenter: TabTrayContract.Presenter
    private lateinit var adapter: TabTrayAdapter
    private lateinit var itemDecoration: ShoppingSearchItemDecoration
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var tabTrayViewModel: TabTrayViewModel
    private lateinit var homeViewModel: HomeViewModel

    private var closeShoppingSearchDialog: AlertDialog? = null
    private var closeTabsDialog: AlertDialog? = null
    private var backgroundDrawable: Drawable? = null
    private var backgroundOverlay: Drawable? = null
    private var playEnterAnimation = true
    private val uiHandler = Handler(Looper.getMainLooper())
    private val slideCoordinator = SlideAnimationCoordinator(this)
    private val dismissRunnable = Runnable { dismissAllowingStateLoss() }
    private var showShoppingSearch = false
    private var onDismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        this.appComponent().inject(this)
        super.onCreate(savedInstanceState)
        tabTrayViewModel = getActivityViewModel(tabTrayViewModelCreator)
        homeViewModel = getActivityViewModel(homeViewModelCreator)
        setStyle(STYLE_NO_TITLE, R.style.TabTrayTheme)
        adapter = TabTrayAdapter(Glide.with(this))
        val sessionManager = TabsSessionProvider.getOrThrow(activity)
        presenter = TabTrayPresenter(this, TabsSessionModel(sessionManager))
        itemDecoration = ShoppingSearchItemDecoration(
            ContextCompat.getDrawable(requireContext(), R.drawable.tab_tray_item_divider),
            ContextCompat.getDrawable(requireContext(), R.drawable.tab_tray_item_divider_night))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tab_tray, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setNightModeEnabled(Settings.getInstance(view.context).isNightModeEnable)
        initWindowBackground(view.context)
        setupBottomSheetCallback()
        prepareExpandAnimation()
        initRecyclerView()
        observeTabTrayAction()

        new_tab_button.setOnClickListener(this)
        close_all_tabs_btn.setOnClickListener(this)
        private_browsing_btn.setOnClickListener(this)
        setupTapBackgroundToExpand()
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                startExpandAnimation()
                presenter.viewReady()
                return false
            }
        })
    }

    override fun onResume() {
        super.onResume()
        tabTrayViewModel.hasPrivateTab().value = getInstance(requireContext()).hasPrivateSession()
        tabTrayViewModel.checkShoppingSearchMode(requireContext())
    }

    override fun onStart() {
        if (playEnterAnimation) {
            playEnterAnimation = false
            setDialogAnimation(R.style.TabTrayDialogEnterExit)
        } else {
            setDialogAnimation(R.style.TabTrayDialogExit)
        }
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        if (closeShoppingSearchDialog?.isShowing == true) {
            closeShoppingSearchDialog?.dismiss()
        }
        if (closeTabsDialog?.isShowing == true) {
            closeTabsDialog?.dismiss()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateBottomBarLayout()
    }

    private fun updateBottomBarLayout() {
        val bottomBarHeight = new_tab_button.resources.getDimensionPixelOffset(R.dimen.tab_tray_new_tab_btn_height)
        new_tab_button.layoutParams = new_tab_button.layoutParams.apply {
            height = bottomBarHeight
        }
        val bottomBarMarginStart = private_browsing_btn.resources.getDimensionPixelOffset(R.dimen.tab_tray_bottom_sheet_start_margin)
        private_browsing_btn.layoutParams = (private_browsing_btn.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = bottomBarMarginStart
        }
        val bottomBarMarginEnd = close_all_tabs_btn.resources.getDimensionPixelOffset(R.dimen.tab_tray_bottom_sheet_end_margin)
        close_all_tabs_btn.layoutParams = (close_all_tabs_btn.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginEnd = bottomBarMarginEnd
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.new_tab_button -> onNewTabClicked()
            R.id.close_all_tabs_btn -> onCloseAllTabsClicked()
            R.id.private_browsing_btn -> {
                privateModeTray(isInLandscape)
                startActivity(PrivateModeActivity.getStartIntent(requireContext()))
                activity?.overridePendingTransition(R.anim.pb_enter, R.anim.pb_exit)
            }
            else -> {
            }
        }
    }

    private val isInLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onShoppingSearchClick() {
        presenter.shoppingSearchClicked()
    }

    override fun onShoppingSearchCloseClick() {
        if (closeShoppingSearchDialog == null) {
            val builder = AlertDialog.Builder(activity)
            closeShoppingSearchDialog = builder.setMessage(R.string.shopping_closing_dialog_body)
                .setPositiveButton(R.string.shopping_closing_dialog_close) { _: DialogInterface?, _: Int ->
                    tabTrayViewModel.finishShoppingSearchMode(requireContext())
                    presenter.shoppingSearchCloseClicked()
                }
                .setNegativeButton(R.string.shopping_closing_dialog_cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
        } else {
            closeShoppingSearchDialog?.show()
        }
    }

    override fun onTabClick(tabPosition: Int) {
        presenter.tabClicked(tabPosition)
        clickTabFromTabTray(isInLandscape)
    }

    override fun onTabCloseClick(tabPosition: Int) {
        presenter.tabCloseClicked(tabPosition)
        closeTabFromTabTray(isInLandscape)
    }

    override fun initData(newTabs: List<Session>, newFocusedTab: Session?) {
        adapter.data = newTabs
        adapter.focusedTab = newFocusedTab
    }

    override fun refreshData(newTabs: List<Session>, newFocusedTab: Session?) {
        val oldTabs = adapter.data
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                return if (showShoppingSearch) oldTabs.size + 1 else oldTabs.size
            }

            override fun getNewListSize(): Int {
                return if (showShoppingSearch) newTabs.size + 1 else newTabs.size
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return if (showShoppingSearch) {
                    if (oldItemPosition == 0 && newItemPosition == 0) {
                        true
                    } else if (oldItemPosition == 0 && newItemPosition != 0 ||
                        oldItemPosition != 0 && newItemPosition == 0) {
                        false
                    } else {
                        newTabs[newItemPosition - 1].id == oldTabs[oldItemPosition - 1].id
                    }
                } else {
                    newTabs[newItemPosition].id == oldTabs[oldItemPosition].id
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return true
            }
        }, false).dispatchUpdatesTo(adapter)
        adapter.data = newTabs
        waitItemAnimation(Runnable {
            val oldFocused = adapter.focusedTab
            val oldTabs1 = adapter.data
            val oldFocusedPosition = oldTabs1.indexOf(oldFocused)
            adapter.notifyItemChanged(if (showShoppingSearch) oldFocusedPosition + 1 else oldFocusedPosition)
            adapter.focusedTab = newFocusedTab
            val newFocusedPosition = oldTabs1.indexOf(newFocusedTab)
            adapter.notifyItemChanged(if (showShoppingSearch) newFocusedPosition + 1 else newFocusedPosition)
        })
    }

    override fun refreshTabData(tab: Session) {
        val tabs = adapter.data
        val position = tabs.indexOf(tab)
        if (position >= 0 && position < tabs.size) {
            adapter.notifyItemChanged(if (showShoppingSearch) position + 1 else position)
        }
    }

    override fun showFocusedTab(tabPosition: Int) {
        layoutManager.scrollToPositionWithOffset(tabPosition,
            tab_tray_recycler_view.measuredHeight / 2)
    }

    override fun tabSwitched(tabPosition: Int) {
        ScreenNavigator.get(context).raiseBrowserScreen(false)
        postOnNextFrame(dismissRunnable)
    }

    override fun closeTabTray() {
        postOnNextFrame(dismissRunnable)
    }

    override fun navigateToHome() {
        ScreenNavigator.get(context).popToHomeScreen(false)
    }

    override fun navigateToShoppingSearch() {
        startActivity(getStartIntent(requireContext()))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(dialog)
        presenter.tabTrayClosed()
    }

    fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        onDismissListener = listener
    }

    private fun setupBottomSheetCallback() {
        val behavior = getBehavior(tab_tray_recycler_view) ?: return
        behavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                slideCoordinator.onSlide(slideOffset)
            }
        })
    }

    private fun initRecyclerView() {
        initRecyclerViewStyle(tab_tray_recycler_view)
        setupSwipeToDismiss(tab_tray_recycler_view)
        adapter.setTabClickListener(this)
        tab_tray_recycler_view.adapter = adapter
    }

    private fun observeTabTrayAction() {
        tabTrayViewModel.hasPrivateTab().observe(viewLifecycleOwner, Observer { hasPrivateTab: Boolean ->
            // Update the UI, in this case, a TextView.
            badge_in_private_mode.visibility = if (hasPrivateTab) View.VISIBLE else View.INVISIBLE
        })
        tabTrayViewModel.uiModel.observe(viewLifecycleOwner, Observer { (showShoppingSearchNewState, keyword) ->
            val isDiff = showShoppingSearch xor showShoppingSearchNewState
            if (isDiff) {
                showShoppingSearch = showShoppingSearchNewState
                presenter.setShoppingSearch(showShoppingSearch)
                if (showShoppingSearch) {
                    tab_tray_recycler_view.addItemDecoration(itemDecoration)
                    adapter.notifyItemInserted(0)
                } else {
                    tab_tray_recycler_view.removeItemDecoration(itemDecoration)
                    adapter.notifyItemRemoved(0)
                }
                adapter.setShoppingSearch(showShoppingSearch, keyword)
            }
        })
    }

    private fun setupSwipeToDismiss(recyclerView: RecyclerView?) {
        val swipeFlag = if (ENABLE_SWIPE_TO_DISMISS) ItemTouchHelper.START or ItemTouchHelper.END else 0
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, swipeFlag) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder is ShoppingSearchViewHolder) {
                    tabTrayViewModel.finishShoppingSearchMode(requireContext())
                    presenter.shoppingSearchCloseClicked()
                } else if (viewHolder is TabViewHolder) {
                    presenter.tabCloseClicked(viewHolder.originPosition)
                    swipeTabFromTabTray(isInLandscape)
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val alpha = 1f - Math.abs(dX) / (recyclerView.width / 2f)
                    viewHolder.itemView.alpha = alpha
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun prepareExpandAnimation() {
        bottomSheetState = BottomSheetBehavior.STATE_HIDDEN

        // update logo-man and background alpha state
        slideCoordinator.onSlide(-1f)
        logo_man.visibility = View.INVISIBLE
    }

    private fun startExpandAnimation() {
        val tabs = adapter.data
        val focusedPosition = tabs.indexOf(adapter.focusedTab)
        val shouldExpand = isPositionVisibleWhenCollapse(if (showShoppingSearch) focusedPosition + 1 else focusedPosition)
        uiHandler.postDelayed({
            if (isVisible) {
                if (shouldExpand) {
                    bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
                    logo_man.visibility = View.VISIBLE
                    setIntercept(false)
                } else {
                    bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
                    setIntercept(true)
                }
            }
        }, resources.getInteger(R.integer.tab_tray_transition_time).toLong())
    }

    private fun isPositionVisibleWhenCollapse(focusedPosition: Int): Boolean {
        val res = resources
        val visiblePanelHeight = res.getDimensionPixelSize(R.dimen.tab_tray_peekHeight) -
            res.getDimensionPixelSize(R.dimen.tab_tray_new_tab_btn_height)
        val itemHeightWithDivider = res.getDimensionPixelSize(R.dimen.tab_tray_item_height) +
            res.getDimensionPixelSize(R.dimen.tab_tray_item_space)
        val visibleItemCount = visiblePanelHeight / itemHeightWithDivider
        return focusedPosition < visibleItemCount
    }

    private fun waitItemAnimation(onAnimationEnd: Runnable) {
        uiHandler.post {
            if (isVisible) {
                val animator = tab_tray_recycler_view.itemAnimator ?: return@post
                animator.isRunning { uiHandler.post(onAnimationEnd) }
            }
        }
    }

    private fun getBehavior(view: View): InterceptBehavior<*>? {
        val params = view.layoutParams as? CoordinatorLayout.LayoutParams ?: return null
        val behavior = params.behavior ?: return null
        return if (behavior is InterceptBehavior<*>) {
            behavior
        } else null
    }

    private var bottomSheetState: Int
        get() {
            val behavior: BottomSheetBehavior<*>? = getBehavior(tab_tray_recycler_view)
            return behavior?.state ?: -1
        }
        private set(state) {
            val behavior: BottomSheetBehavior<*>? = getBehavior(tab_tray_recycler_view)
            if (behavior != null) {
                behavior.state = state
            }
        }

    private val collapseHeight: Int
        get() {
            val behavior: BottomSheetBehavior<*>? = getBehavior(tab_tray_recycler_view)
            return behavior?.peekHeight ?: 0
        }

    private fun setIntercept(intercept: Boolean) {
        val behavior = getBehavior(tab_tray_recycler_view)
        behavior?.setIntercept(intercept)
    }

    private fun initRecyclerViewStyle(recyclerView: RecyclerView) {
        val context = recyclerView.context
        recyclerView.layoutManager = LinearLayoutManager(context,
            RecyclerView.VERTICAL, false).also { layoutManager = it }
        val animator = recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    private fun setupTapBackgroundToExpand() {
        val detector = GestureDetectorCompat(context,
            object : SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }
            })
        root_layout.setOnTouchListener { v: View, event: MotionEvent? ->
            val result = detector.onTouchEvent(event)
            if (result) {
                v.performClick()
            }
            result
        }
    }

    private fun onNewTabClicked() {
        ScreenNavigator.get(context).addHomeScreen(false)
        homeViewModel.onNewTabButtonClicked()
        clickAddTabTray(isInLandscape)
        postOnNextFrame(dismissRunnable)
    }

    private fun onCloseAllTabsClicked() {
        if (closeTabsDialog == null) {
            val builder = AlertDialog.Builder(activity)
            closeTabsDialog = builder.setMessage(R.string.tab_tray_close_tabs_dialog_msg)
                .setPositiveButton(R.string.action_ok) { _: DialogInterface?, _: Int ->
                    presenter.closeAllTabs()
                    closeAllTabFromTabTray(isInLandscape)
                }
                .setNegativeButton(R.string.action_cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .show()
        } else {
            closeTabsDialog?.show()
        }
    }

    private fun initWindowBackground(context: Context) {
        val drawable = context.getDrawable(R.drawable.tab_tray_background)
        if (drawable == null) {
            if (BuildConfig.DEBUG) {
                throw RuntimeException("fail to resolve background drawable")
            }
            return
        }
        if (drawable is LayerDrawable) {
            val layerDrawable = drawable
            if (Settings.getInstance(getContext()).isNightModeEnable) {
                backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.gradient_background_night)
                // set alpha = 0 to let this layer invisible
                layerDrawable.findDrawableByLayerId(R.id.gradient_background).alpha = 0
            } else {
                backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.gradient_background)
                layerDrawable.findDrawableByLayerId(R.id.gradient_background_night).alpha = 0
            }
            backgroundOverlay = layerDrawable.findDrawableByLayerId(R.id.background_overlay)
            val alpha = validateBackgroundAlpha(0xff)
            backgroundDrawable?.alpha = alpha
            backgroundOverlay?.alpha = if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) 0 else (alpha * OVERLAY_ALPHA_FULL_EXPANDED).toInt()
        } else {
            backgroundDrawable = drawable
        }
        dialog?.window?.setBackgroundDrawable(drawable)
    }

    private fun validateBackgroundAlpha(alpha: Int): Int {
        return Math.max(Math.min(alpha, 0xfe), 0x01)
    }

    private fun setDialogAnimation(@StyleRes resId: Int) {
        dialog?.window?.let {
            it.attributes.windowAnimations = resId
            updateWindowAttrs(it)
        }
    }

    private fun updateWindowAttrs(window: Window) {
        context?.let {
            val manager = it.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val decor = window.decorView
            if (decor.isAttachedToWindow) {
                manager.updateViewLayout(decor, window.attributes)
            }
        }
    }

    private fun onTranslateToHidden(translationY: Float) {
        new_tab_button.translationY = translationY
        logo_man.translationY = translationY
    }

    private fun updateWindowBackground(backgroundAlpha: Float) {
        root_layout.alpha = backgroundAlpha
        backgroundDrawable?.alpha = validateBackgroundAlpha((backgroundAlpha * 0xff).toInt())
    }

    private fun updateWindowOverlay(overlayAlpha: Float) {
        backgroundOverlay?.alpha = validateBackgroundAlpha((overlayAlpha * 0xff).toInt())
    }

    private fun onFullyExpanded() {
        if (logo_man.visibility != View.VISIBLE) {
            // We don't want to show logo-man during fully expand animation (too complex visually).
            // In this case, we hide logo-man at first, and make sure it become visible after tab
            // tray is fully expanded (slideOffset >= 1). See prepareExpandAnimation()
            logo_man.visibility = View.VISIBLE
        }
        setIntercept(false)
    }

    private fun postOnNextFrame(runnable: Runnable) {
        uiHandler.post { uiHandler.post(runnable) }
    }

    private class SlideAnimationCoordinator internal constructor(private val fragment: TabTrayFragment) {
        private val backgroundInterpolator: Interpolator = AccelerateInterpolator()
        private val overlayInterpolator: Interpolator = AccelerateDecelerateInterpolator()
        private var collapseHeight = -1
        private var translationY = Int.MIN_VALUE.toFloat()
        private var backgroundAlpha = -1f
        private var overlayAlpha = -1f
        fun onSlide(slideOffset: Float) {
            if (!fragment.isVisible) {
                return
            }
            var backgroundAlpha = 1f
            var overlayAlpha = 0f
            var translationY = 0f
            if (slideOffset < 0) {
                if (collapseHeight < 0) {
                    collapseHeight = fragment.collapseHeight
                }
                translationY = collapseHeight * -slideOffset
                if (ENABLE_BACKGROUND_ALPHA_TRANSITION) {
                    val interpolated = backgroundInterpolator.getInterpolation(-slideOffset)
                    backgroundAlpha = Math.max(0f, 1 - interpolated)
                }
            } else {
                val interpolated = overlayInterpolator.getInterpolation(1 - slideOffset)
                overlayAlpha = -(interpolated * OVERLAY_ALPHA_FULL_EXPANDED) + OVERLAY_ALPHA_FULL_EXPANDED
            }
            if (slideOffset >= 1) {
                fragment.onFullyExpanded()
            }
            if (java.lang.Float.compare(this.translationY, translationY) != 0) {
                this.translationY = translationY
                fragment.onTranslateToHidden(translationY)
            }
            if (java.lang.Float.compare(this.backgroundAlpha, backgroundAlpha) != 0) {
                this.backgroundAlpha = backgroundAlpha
                fragment.updateWindowBackground(backgroundAlpha)
            }
            if (java.lang.Float.compare(this.overlayAlpha, overlayAlpha) != 0) {
                this.overlayAlpha = overlayAlpha
                fragment.updateWindowOverlay(overlayAlpha)
            }
        }
    }

    private class ShoppingSearchItemDecoration internal constructor(private val divierDefault: Drawable?, private val divierNight: Drawable?) : RecyclerView.ItemDecoration() {
        private val bounds = Rect()
        private var isNight = false
        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val divider = if (isNight) divierNight else divierDefault
            if (parent.layoutManager == null || divider == null) {
                return
            }
            c.save()
            val left: Int
            val right: Int
            if (parent.clipToPadding) {
                left = parent.paddingLeft
                right = parent.width - parent.paddingRight
                c.clipRect(left, parent.paddingTop, right,
                    parent.height - parent.paddingBottom)
            } else {
                left = 0
                right = parent.width
            }
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(0)
                if (parent.getChildAdapterPosition(child) == 0) {
                    parent.getDecoratedBoundsWithMargins(child, bounds)
                    val bottom = bounds.bottom + Math.round(child.translationY)
                    val top = bottom - divider.intrinsicHeight
                    divider.setBounds(left, top, right, bottom)
                    divider.draw(c)
                }
            }
            c.restore()
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.getChildAdapterPosition(view) == 1) {
                outRect.top = view.resources.getDimensionPixelOffset(R.dimen.tab_tray_padding)
            }
        }

        fun setNightMode(enable: Boolean) {
            isNight = enable
        }
    }

    private fun setNightModeEnabled(enable: Boolean) {
        new_tab_button.setNightMode(enable)
        private_browsing_img.setNightMode(enable)
        plus_sign.setNightMode(enable)
        bottom_divider.setNightMode(enable)
        itemDecoration.setNightMode(enable)
        tab_tray_recycler_view.setNightMode(enable)
        dialog?.window?.let {
            ViewUtils.updateStatusBarStyle(!enable, it)
        }
        star_background.visibility = if (enable) View.VISIBLE else View.GONE
    }

    companion object {
        const val FRAGMENT_TAG = "tab_tray"
        private const val ENABLE_BACKGROUND_ALPHA_TRANSITION = true
        private const val ENABLE_SWIPE_TO_DISMISS = true
        private const val OVERLAY_ALPHA_FULL_EXPANDED = 0.50f

        @JvmStatic
        fun newInstance(): TabTrayFragment {
            return TabTrayFragment()
        }
    }
}