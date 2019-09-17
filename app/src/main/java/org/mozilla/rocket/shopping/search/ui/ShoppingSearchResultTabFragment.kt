package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_result_tab.*
import kotlinx.android.synthetic.main.toolbar.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.AppConstants
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabHelper
import org.mozilla.rocket.content.common.ui.ContentTabViewContract
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.shopping.search.data.ShoppingSearchMode
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchTabsAdapter.TabItem
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import javax.inject.Inject

class ShoppingSearchResultTabFragment : Fragment(), ContentTabViewContract {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchResultViewModel>

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    @Inject
    lateinit var bottomBarViewModelCreator: Lazy<ShoppingSearchBottomBarViewModel>

    private lateinit var shoppingSearchResultViewModel: ShoppingSearchResultViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var contentTabHelper: ContentTabHelper
    private lateinit var contentTabObserver: ContentTabHelper.Observer
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private val safeArgs: ShoppingSearchResultTabFragmentArgs by navArgs()
    private val searchKeyword by lazy { safeArgs.searchKeyword }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        shoppingSearchResultViewModel = getViewModel(viewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_result_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomBar(view)

        appbar.setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as LinearLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewPager()
        initTabLayout()

        contentTabHelper = ContentTabHelper(this)
        contentTabHelper.initPermissionHandler()
        contentTabObserver = contentTabHelper.getObserver()
        sessionManager = TabsSessionProvider.getOrThrow(activity)
        sessionManager.register(contentTabObserver)
        sessionManager.focusSession?.register(contentTabObserver)

        observeChromeAction()

        shoppingSearchResultViewModel.search(searchKeyword)

        ShoppingSearchMode.getInstance(requireContext()).saveKeyword(searchKeyword)

        observeAction()
    }

    private fun observeAction() {
        shoppingSearchResultViewModel.showOnboardingDialog.observe(this, Observer {
            val dialogFragment = ShoppingSearchContentSwitchOnboardingDialogFragment()
            dialogFragment.show(childFragmentManager, "onboardingDialogFragment")
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        sessionManager.focusSession?.unregister(contentTabObserver)
        sessionManager.unregister(contentTabObserver)
    }

    override fun getHostActivity() = activity as AppCompatActivity

    override fun getCurrentSession() = sessionManager.focusSession

    override fun getChromeViewModel() = chromeViewModel

    override fun getSiteIdentity(): ImageView? = site_identity

    override fun getDisplayUrlView(): TextView? = display_url

    override fun getProgressBar(): ProgressBar? = progress

    override fun getFullScreenGoneViews() = listOf(toolbar_root, bottom_bar, tab_layout)

    override fun getFullScreenInvisibleViews() = listOf(view_pager)

    override fun getFullScreenContainerView(): ViewGroup = video_container

    private fun setupBottomBar(rootView: View) {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_HOME -> sendHomeIntent(requireContext())
                    BottomBarItemAdapter.TYPE_REFRESH -> chromeViewModel.refreshOrStop.call()
                    BottomBarItemAdapter.TYPE_SHOPPING_SEARCH -> sendNewSearchIntent(requireContext())
                    BottomBarItemAdapter.TYPE_NEXT -> chromeViewModel.goNext.call()
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.ShoppingSearch)
        val bottomBarViewModel = getActivityViewModel(bottomBarViewModelCreator)
        bottomBarViewModel.items.nonNullObserve(this) {
            bottomBarItemAdapter.setItems(it)
        }

        chromeViewModel.isRefreshing.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(bottomBarViewModel.items)
            .observe(this, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
    }

    private fun initViewPager() {
        shoppingSearchResultViewModel.shoppingSearchSites.observe(this, Observer { shoppingSearchSites ->
            val tabItems = shoppingSearchSites.map { site ->
                TabItem(site.title, site.searchUrl)
            }
            view_pager.adapter = ShoppingSearchTabsAdapter(childFragmentManager, tabItems)
        })
    }

    private fun initTabLayout() {
        tab_layout.setupWithViewPager(view_pager)

        preferenceButton.setOnClickListener {
            shoppingSearchResultViewModel.goPreferences.call()
        }

        shoppingSearchResultViewModel.goPreferences.observe(this, Observer {
            activity?.baseContext?.let {
                startActivity(ShoppingSearchPreferencesActivity.getStartIntent(it))
            }
        })
    }

    private fun observeChromeAction() {
        chromeViewModel.refreshOrStop.observe(this, Observer {
            if (chromeViewModel.isRefreshing.value == true) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(this, Observer {
            if (chromeViewModel.canGoForward.value == true) {
                goForward()
            }
        })
        chromeViewModel.share.observe(this, Observer {
            sendShareIntent()
        })
    }

    private fun sendHomeIntent(context: Context) {
        val intent = Intent().apply {
            setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS)
        }
        startActivity(intent)
    }

    private fun sendNewSearchIntent(context: Context) {
        startActivity(
            ShoppingSearchActivity.getStartIntent(context).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
    }

    private fun sendShareIntent() {
        val list = shoppingSearchResultViewModel.shoppingSearchSites.value
        list?.apply {
            val index = view_pager.currentItem
            val item = list[index]
            val subject = item.title
            val text = item.title
            val share = Intent(Intent.ACTION_SEND)

            share.type = "text/plain"
            share.putExtra(Intent.EXTRA_SUBJECT, subject)
            share.putExtra(Intent.EXTRA_TEXT, text)

            startActivity(share)
        }
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()

    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()

    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()

    private fun reload() = sessionManager.focusSession?.engineSession?.reload()
}
