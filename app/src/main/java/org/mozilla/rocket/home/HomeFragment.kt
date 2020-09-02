package org.mozilla.rocket.home

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import kotlinx.android.synthetic.main.button_menu.menu_red_dot
import kotlinx.android.synthetic.main.fragment_home.account_layout
import kotlinx.android.synthetic.main.fragment_home.arc_panel
import kotlinx.android.synthetic.main.fragment_home.arc_view
import kotlinx.android.synthetic.main.fragment_home.home_background
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input_icon
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input_text
import kotlinx.android.synthetic.main.fragment_home.home_fragment_menu_button
import kotlinx.android.synthetic.main.fragment_home.home_fragment_tab_counter
import kotlinx.android.synthetic.main.fragment_home.logo_man_notification
import kotlinx.android.synthetic.main.fragment_home.main_list
import kotlinx.android.synthetic.main.fragment_home.page_indicator
import kotlinx.android.synthetic.main.fragment_home.private_mode_button
import kotlinx.android.synthetic.main.fragment_home.profile_button
import kotlinx.android.synthetic.main.fragment_home.reward_button
import kotlinx.android.synthetic.main.fragment_home.search_panel
import kotlinx.android.synthetic.main.fragment_home.shopping_button
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.FirebaseHelper.stopAndClose
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.component.RocketLauncherActivity
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.extension.showFxToast
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.fxa.ProfileActivity
import org.mozilla.rocket.home.logoman.ui.LogoManNotification
import org.mozilla.rocket.home.topsites.domain.PinTopSiteUseCase
import org.mozilla.rocket.home.topsites.ui.AddNewTopSitesActivity
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage
import org.mozilla.rocket.home.topsites.ui.SitePageAdapterDelegate
import org.mozilla.rocket.home.topsites.ui.SiteViewHolder.Companion.TOP_SITE_LONG_CLICK_TARGET
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_DEFAULT
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_DOWNLOADING
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_UNREAD
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_WARNING
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.ui.RewardActivity
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserHelper
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.util.setCurrentItem
import javax.inject.Inject

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelCreator: Lazy<HomeViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var downloadIndicatorViewModelCreator: Lazy<DownloadIndicatorViewModel>
    @Inject
    lateinit var defaultBrowserPreferenceViewModelCreator: Lazy<DefaultBrowserPreferenceViewModel>
    @Inject
    lateinit var appContext: Context

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var downloadIndicatorViewModel: DownloadIndicatorViewModel
    private lateinit var defaultBrowserPreferenceViewModel: DefaultBrowserPreferenceViewModel
    private lateinit var themeManager: ThemeManager
    private lateinit var topSitesAdapter: DelegateAdapter
    private lateinit var defaultBrowserHelper: DefaultBrowserHelper
    private var currentShoppingBtnVisibleState = false

    private val topSitesPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            homeViewModel.onTopSitesPagePositionChanged(position)
        }
    }
    private val toastObserver = Observer<ToastMessage> {
        appContext.showFxToast(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        homeViewModel = getActivityViewModel(homeViewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        downloadIndicatorViewModel = getActivityViewModel(downloadIndicatorViewModelCreator)
        defaultBrowserPreferenceViewModel = getActivityViewModel(defaultBrowserPreferenceViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        themeManager = (context as ThemeManager.ThemeHost).themeManager
        initSearchToolBar()
        initBackgroundView()
        initTopSites()
        initFxaView()
        initLogoManNotification()
        observeDarkTheme()
        initOnboardingSpotlight()
        observeAddNewTopSites()
        observeSetDefaultBrowser()
        observeActions()

        Looper.myQueue().addIdleHandler {
            FirebaseHelper.retrieveTrace("coldStart")?.stopAndClose()
            false
        }
    }

    private fun initSearchToolBar() {
        home_fragment_fake_input.setOnClickListener {
            chromeViewModel.showUrlInput.call()
            TelemetryWrapper.showSearchBarHome()
        }
        home_fragment_menu_button.apply {
            setOnClickListener {
                chromeViewModel.showHomeMenu.call()
                TelemetryWrapper.showMenuHome()
            }
            setOnLongClickListener {
                chromeViewModel.showDownloadPanel.call()
                TelemetryWrapper.longPressDownloadIndicator()
                true
            }
        }
        home_fragment_tab_counter.setOnClickListener {
            chromeViewModel.showTabTray.call()
            TelemetryWrapper.showTabTrayHome()
        }
        chromeViewModel.tabCount.observe(viewLifecycleOwner, Observer {
            setTabCount(it ?: 0)
        })
        homeViewModel.isShoppingSearchEnabled.observe(viewLifecycleOwner, Observer { isEnabled ->
            shopping_button.isVisible = isEnabled
            private_mode_button.isVisible = !isEnabled
        })
        shopping_button.setOnClickListener { homeViewModel.onShoppingButtonClicked() }
        homeViewModel.openShoppingSearch.observe(viewLifecycleOwner, Observer {
            showShoppingSearch()
        })
        chromeViewModel.isPrivateBrowsingActive.observe(viewLifecycleOwner, Observer {
            private_mode_button.isActivated = it
        })
        private_mode_button.setOnClickListener { homeViewModel.onPrivateModeButtonClicked() }
        homeViewModel.openPrivateMode.observe(viewLifecycleOwner, Observer {
            chromeViewModel.togglePrivateMode.call()
        })
        homeViewModel.shouldShowNewMenuItemHint.switchMap {
            if (it) {
                MutableLiveData<DownloadIndicatorViewModel.Status>().apply { DownloadIndicatorViewModel.Status.DEFAULT }
            } else {
                downloadIndicatorViewModel.downloadIndicatorObservable
            }
        }.observe(viewLifecycleOwner, Observer {
            home_fragment_menu_button.apply {
                when (it) {
                    DownloadIndicatorViewModel.Status.DOWNLOADING -> setDownloadState(DOWNLOAD_STATE_DOWNLOADING)
                    DownloadIndicatorViewModel.Status.UNREAD -> setDownloadState(DOWNLOAD_STATE_UNREAD)
                    DownloadIndicatorViewModel.Status.WARNING -> setDownloadState(DOWNLOAD_STATE_WARNING)
                    else -> setDownloadState(DOWNLOAD_STATE_DEFAULT)
                }
            }
        })
        homeViewModel.shouldShowNewMenuItemHint.observe(viewLifecycleOwner, Observer {
            menu_red_dot.isVisible = it
        })
    }

    private fun initBackgroundView() {
        themeManager.subscribeThemeChange(home_background)
        val backgroundGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return homeViewModel.onBackgroundViewDoubleTap()
            }

            override fun onLongPress(e: MotionEvent?) {
                homeViewModel.onBackgroundViewLongPress()
            }
        })
        home_background.setOnTouchListener { _, event ->
            backgroundGestureDetector.onTouchEvent(event)
        }
        homeViewModel.toggleBackgroundColor.observe(viewLifecycleOwner, Observer {
            val themeSet = themeManager.toggleNextTheme()
            TelemetryWrapper.changeThemeTo(themeSet.name)
        })
        homeViewModel.resetBackgroundColor.observe(viewLifecycleOwner, Observer {
            themeManager.resetDefaultTheme()
            TelemetryWrapper.resetThemeToDefault()
        })
        homeViewModel.homeBackgroundColorThemeClicked.observe(viewLifecycleOwner, Observer { themeSet ->
            themeManager.setCurrentTheme(themeSet)
        })
    }

    private fun initTopSites() {
        topSitesAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(SitePage::class, R.layout.item_top_site_page, SitePageAdapterDelegate(homeViewModel, chromeViewModel))
            }
        )
        main_list.apply {
            adapter = this@HomeFragment.topSitesAdapter
            registerOnPageChangeCallback(topSitesPageChangeCallback)
        }
        var savedTopSitesPagePosition = homeViewModel.topSitesPageIndex.value
        homeViewModel.run {
            sitePages.observe(viewLifecycleOwner, Observer {
                page_indicator.setSize(it.size)
                topSitesAdapter.setData(it)
                savedTopSitesPagePosition?.let { savedPosition ->
                    savedTopSitesPagePosition = null
                    main_list.setCurrentItem(savedPosition, false)
                }
            })
            topSitesPageIndex.observe(viewLifecycleOwner, Observer {
                page_indicator.setSelection(it)
            })
            openBrowser.observe(viewLifecycleOwner, Observer { url ->
                ScreenNavigator.get(context).showBrowserScreen(url, true, false)
            })
            showTopSiteMenu.observe(viewLifecycleOwner, Observer { (site, position) ->
                site as Site.UrlSite.RemovableSite
                val anchorView = main_list.findViewWithTag<View>(TOP_SITE_LONG_CLICK_TARGET).apply { tag = null }
                val allowToPin = !site.isPinned
                showTopSiteMenu(anchorView, allowToPin, site, position)
            })
            showAddTopSiteMenu.observe(viewLifecycleOwner, Observer {
                val anchorView = main_list.findViewWithTag<View>(TOP_SITE_LONG_CLICK_TARGET).apply { tag = null }
                showAddTopSiteMenu(anchorView)
            })
        }
        chromeViewModel.clearBrowsingHistory.observe(viewLifecycleOwner, Observer {
            homeViewModel.onClearBrowsingHistory()
        })
    }

    private fun initFxaView() {
        homeViewModel.isAccountLayerVisible.observe(viewLifecycleOwner, Observer {
            account_layout.isVisible = it
        })
        homeViewModel.hasUnreadMissions.observe(viewLifecycleOwner, Observer {
            reward_button.isActivated = it
        })
        homeViewModel.isFxAccount.observe(viewLifecycleOwner, Observer {
            profile_button.isActivated = it
        })
        reward_button.setOnClickListener { homeViewModel.onRewardButtonClicked() }
        profile_button.setOnClickListener { homeViewModel.onProfileButtonClicked() }
    }

    private fun observeDarkTheme() {
        chromeViewModel.isDarkTheme.observe(viewLifecycleOwner, Observer { darkThemeEnable ->
            ViewUtils.updateStatusBarStyle(!darkThemeEnable, requireActivity().window)
            topSitesAdapter.notifyDataSetChanged()
            home_background.setDarkTheme(darkThemeEnable)
            arc_view.setDarkTheme(darkThemeEnable)
            arc_panel.setDarkTheme(darkThemeEnable)
            search_panel.setDarkTheme(darkThemeEnable)
            home_fragment_fake_input.setDarkTheme(darkThemeEnable)
            home_fragment_fake_input_icon.setDarkTheme(darkThemeEnable)
            home_fragment_fake_input_text.setDarkTheme(darkThemeEnable)
            home_fragment_tab_counter.setDarkTheme(darkThemeEnable)
            home_fragment_menu_button.setDarkTheme(darkThemeEnable)
            account_layout.setDarkTheme(darkThemeEnable)
            shopping_button.setDarkTheme(darkThemeEnable)
            private_mode_button.setDarkTheme(darkThemeEnable)
        })
    }

    override fun onStart() {
        super.onStart()
        homeViewModel.onPageForeground()
    }

    override fun onResume() {
        super.onResume()
        defaultBrowserPreferenceViewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        defaultBrowserPreferenceViewModel.onPause()
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.onPageBackground()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        themeManager.unsubscribeThemeChange(home_background)
        main_list.unregisterOnPageChangeCallback(topSitesPageChangeCallback)
        homeViewModel.showToast.removeObserver(toastObserver)
    }

    override fun getFragment(): Fragment = this

    override fun onUrlInputScreenVisible(visible: Boolean) {
        if (visible) {
            chromeViewModel.onShowHomePageUrlInput()
        } else {
            chromeViewModel.onDismissHomePageUrlInput()
        }
    }

    override fun applyLocale() {
        home_fragment_fake_input_text.text = getString(R.string.home_search_bar_text)
    }

    fun notifyAddNewTopSiteResult(pinTopSiteResult: PinTopSiteUseCase.PinTopSiteResult) {
        homeViewModel.onAddNewTopSiteResult(pinTopSiteResult)
    }

    private fun showTopSiteMenu(anchorView: View, pinEnabled: Boolean, site: Site, position: Int) {
        PopupMenu(anchorView.context, anchorView, Gravity.CLIP_HORIZONTAL)
                .apply {
                    menuInflater.inflate(R.menu.menu_top_site_item, menu)
                    menu.findItem(R.id.pin)?.apply {
                        isVisible = pinEnabled
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.pin -> homeViewModel.onPinTopSiteClicked(site, position)
                            R.id.remove -> homeViewModel.onRemoveTopSiteClicked(site, position)
                            else -> throw IllegalStateException("Unhandled menu item")
                        }

                        true
                    }
                }
                .show()
    }

    private fun showAddTopSiteMenu(anchorView: View) {
        PopupMenu(anchorView.context, anchorView, Gravity.CLIP_HORIZONTAL)
                .apply {
                    menuInflater.inflate(R.menu.menu_add_top_site_item, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.add_top_sites -> homeViewModel.onAddTopSiteContextMenuClicked()
                            else -> throw IllegalStateException("Unhandled menu item")
                        }
                        true
                    }
                }
                .show()
    }

    private fun setTabCount(count: Int, animationEnabled: Boolean = false) {
        home_fragment_tab_counter.apply {
            if (animationEnabled) {
                setCountWithAnimation(count)
            } else {
                setCount(count)
            }
            if (count > 0) {
                isEnabled = true
                alpha = 1f
            } else {
                isEnabled = false
                alpha = 0.3f
            }
        }
    }

    private fun showShoppingSearch() {
        val context: Context = this.context ?: return
        startActivity(ShoppingSearchActivity.getStartIntent(context))
    }

    private fun showAddNewTopSitesPage() {
        activity?.let {
            it.startActivityForResult(AddNewTopSitesActivity.getStartIntent(it), AddNewTopSitesActivity.REQUEST_CODE_ADD_NEW_TOP_SITES)
        }
    }

    private fun initLogoManNotification() {
        homeViewModel.logoManNotification.observe(viewLifecycleOwner, Observer {
            it?.let { (notification, animate) ->
                showLogoManNotification(notification, animate)
            }
        })
        homeViewModel.hideLogoManNotification.observe(viewLifecycleOwner, Observer {
            hideLogoManNotification()
        })
        logo_man_notification.setNotificationActionListener(object : LogoManNotification.NotificationActionListener {
            override fun onNotificationClick() {
                homeViewModel.onLogoManNotificationClicked()
            }

            override fun onNotificationDismiss() {
                homeViewModel.onLogoManDismissed()
            }
        })
    }

    private fun showLogoManNotification(notification: LogoManNotification.Notification, animate: Boolean) {
        logo_man_notification.showNotification(notification, animate)
        homeViewModel.onLogoManShown()
    }

    private fun hideLogoManNotification() {
        logo_man_notification.isVisible = false
    }

    private fun showShoppingSearchSpotlight() {
        val dismissListener = DialogInterface.OnDismissListener {
            restoreStatusBarColor()
            shopping_button?.isVisible = currentShoppingBtnVisibleState
            private_mode_button?.isVisible = !currentShoppingBtnVisibleState
        }
        shopping_button.post {
            if (isAdded) {
                setOnboardingStatusBarColor()
                DialogUtils.showShoppingSearchSpotlight(requireActivity(), shopping_button, dismissListener)
            }
        }
    }

    private fun restoreStatusBarColor() {
        activity?.window?.statusBarColor = Color.TRANSPARENT
    }

    private fun setOnboardingStatusBarColor() {
        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(it, R.color.paletteBlack50)
        }
    }

    private fun initOnboardingSpotlight() {
        homeViewModel.showShoppingSearchOnboardingSpotlight.observe(viewLifecycleOwner, Observer {
            currentShoppingBtnVisibleState = shopping_button.isVisible
            shopping_button.isVisible = true
            private_mode_button.isVisible = false
            showShoppingSearchSpotlight()
        })
    }

    private fun observeAddNewTopSites() {
        homeViewModel.openAddNewTopSitesPage.observe(viewLifecycleOwner, Observer {
            showAddNewTopSitesPage()
        })
        homeViewModel.addNewTopSiteFullyPinned.observe(viewLifecycleOwner, Observer {
            context?.let {
                Toast.makeText(it, R.string.add_top_site_toast, Toast.LENGTH_LONG).show()
            }
        })
        chromeViewModel.addNewTopSiteMenuClicked.observe(viewLifecycleOwner, Observer {
            homeViewModel.onAddTopSiteMenuClicked()
        })
        homeViewModel.addNewTopSiteSuccess.observe(viewLifecycleOwner, Observer { page ->
            page?.let {
                scrollToTopSitePage(it)
            }
            Snackbar.make(main_list, getText(R.string.add_top_site_snackbar_1), Snackbar.LENGTH_LONG)
                .setAction(R.string.add_top_site_button) { homeViewModel.onAddMoreTopSiteSnackBarClicked() }
                .show()
        })
        homeViewModel.addExistingTopSite.observe(viewLifecycleOwner, Observer { page ->
            page?.let {
                scrollToTopSitePage(it)
            }
            Snackbar.make(main_list, getText(R.string.add_top_site_snackbar_2), Snackbar.LENGTH_LONG)
                .setAction(R.string.add_top_site_button) { homeViewModel.onAddMoreTopSiteSnackBarClicked() }
                .show()
        })
    }

    private fun observeSetDefaultBrowser() {
        activity?.let { activity ->
            defaultBrowserHelper = DefaultBrowserHelper(activity, defaultBrowserPreferenceViewModel)
            homeViewModel.tryToSetDefaultBrowser.observe(viewLifecycleOwner, Observer {
                defaultBrowserPreferenceViewModel.performAction()
            })
            defaultBrowserPreferenceViewModel.openDefaultAppsSettings.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.openDefaultAppsSettings() })
            defaultBrowserPreferenceViewModel.openAppDetailSettings.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.openAppDetailSettings() })
            defaultBrowserPreferenceViewModel.openSumoPage.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.openSumoPage() })
            defaultBrowserPreferenceViewModel.triggerWebOpen.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.triggerWebOpen() })
            defaultBrowserPreferenceViewModel.openDefaultAppsSettingsTutorialDialog.observe(viewLifecycleOwner, Observer { DialogUtils.showGoToSystemAppsSettingsDialog(activity, defaultBrowserPreferenceViewModel) })
            defaultBrowserPreferenceViewModel.openUrlTutorialDialog.observe(viewLifecycleOwner, Observer { DialogUtils.showOpenUrlDialog(activity, defaultBrowserPreferenceViewModel) })
            defaultBrowserPreferenceViewModel.successToSetDefaultBrowser.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.showSuccessMessage() })
            defaultBrowserPreferenceViewModel.failToSetDefaultBrowser.observe(viewLifecycleOwner, Observer { defaultBrowserHelper.showFailMessage() })
        }
    }

    private fun scrollToTopSitePage(page: Int) =
        main_list.postDelayed({ main_list.setCurrentItem(page, 300) }, 100)

    private fun observeActions() {
        homeViewModel.showToast.observeForever(toastObserver)
        homeViewModel.openRewardPage.observe(viewLifecycleOwner, Observer {
            openRewardPage()
        })
        homeViewModel.openProfilePage.observe(viewLifecycleOwner, Observer {
            openProfilePage()
        })
        homeViewModel.showMissionCompleteDialog.observe(viewLifecycleOwner, Observer { mission ->
            showMissionCompleteDialog(mission)
        })
        homeViewModel.executeUriAction.observe(viewLifecycleOwner, Observer { action ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action), appContext, RocketLauncherActivity::class.java))
        })
        homeViewModel.openMissionDetailPage.observe(viewLifecycleOwner, Observer { mission ->
            openMissionDetailPage(mission)
        })
        homeViewModel.showKeyboard.observe(viewLifecycleOwner, Observer {
            Looper.myQueue().addIdleHandler {
                if (!isStateSaved) {
                    home_fragment_fake_input.performClick()
                }
                false
            }
        })
        chromeViewModel.themeSettingMenuClicked.observe(viewLifecycleOwner, Observer {
            homeViewModel.onThemeSettingMenuClicked()
        })
        homeViewModel.showThemeSetting.observe(viewLifecycleOwner, Observer {
            activity?.let {
                DialogUtils.showThemeSettingDialog(it, homeViewModel)
            }
        })
        homeViewModel.showSetAsDefaultBrowserOnboarding.observe(viewLifecycleOwner, Observer {
            activity?.let {
                DialogUtils.showSetAsDefaultBrowserDialog(
                    it,
                    { homeViewModel.onSetAsDefaultBrowserClicked() },
                    { homeViewModel.onCancelSetAsDefaultBrowserClicked() }
                )
            }
        })
    }

    private fun showMissionCompleteDialog(mission: Mission) {
        DialogUtils.createMissionCompleteDialog(requireContext(), mission.imageUrl)
                .onPositive {
                    homeViewModel.onRedeemCompletedMissionButtonClicked(mission)
                }
                .onNegative {
                    homeViewModel.onRedeemCompletedLaterButtonClicked()
                }
                .onClose {
                    homeViewModel.onRedeemCompletedDialogClosed()
                }
                .show()
    }

    private fun openRewardPage() {
        startActivity(RewardActivity.getStartIntent(requireContext()))
    }

    private fun openProfilePage() {
        startActivity(ProfileActivity.getStartIntent(requireContext()))
    }

    private fun openMissionDetailPage(mission: Mission) {
        startActivity(RewardActivity.getStartIntent(requireContext(), RewardActivity.DeepLink.MissionDetailPage(mission)))
    }

    companion object {
        private const val TITLE_VERTICAL_BIAS = 0.45f
        private const val TITLE_VERTICAL_BIAS_WITH_CONTENT_HUB = 0.26f
    }
}
