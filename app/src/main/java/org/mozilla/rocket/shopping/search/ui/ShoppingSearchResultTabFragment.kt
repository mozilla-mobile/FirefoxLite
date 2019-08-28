package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabFragment
import org.mozilla.rocket.content.common.ui.ContentTabHelper
import org.mozilla.rocket.content.common.ui.ContentTabViewContract
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
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

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var bottomBar: BottomBar
    private lateinit var videoContainer: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView
    private lateinit var toolbarRoot: ViewGroup

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

        tabLayout = view.findViewById(R.id.shopping_search_tabs)
        viewPager = view.findViewById(R.id.view_pager)
        bottomBar = view.findViewById(R.id.bottom_bar)
        videoContainer = view.findViewById(R.id.video_container)
        displayUrlView = view.findViewById(R.id.display_url)
        siteIdentity = view.findViewById(R.id.site_identity)
        progressView = view.findViewById(R.id.progress)
        toolbarRoot = view.findViewById(R.id.toolbar_root)

        view.findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
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
    }

    override fun onDestroyView() {
        super.onDestroyView()

        sessionManager.focusSession?.unregister(contentTabObserver)
        sessionManager.unregister(contentTabObserver)
    }

    override fun getHostActivity() = activity as AppCompatActivity

    override fun getCurrentSession() = sessionManager.focusSession

    override fun getChromeViewModel() = chromeViewModel

    override fun getSiteIdentity() = siteIdentity

    override fun getDisplayUrlView() = displayUrlView

    override fun getProgressBar() = progressView

    override fun getFullScreenGoneViews() = listOf(toolbarRoot, bottomBar, tabLayout)

    override fun getFullScreenInvisibleViews() = listOf(viewPager)

    override fun getFullScreenContainerView() = videoContainer

    private fun setupBottomBar(rootView: View) {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_PRIVATE_HOME -> activity?.onBackPressed()
                    BottomBarItemAdapter.TYPE_REFRESH -> chromeViewModel.refreshOrStop.call()
                    BottomBarItemAdapter.TYPE_DELETE -> activity?.finishAndRemoveTask()
                    BottomBarItemAdapter.TYPE_NEXT -> chromeViewModel.goNext.call()
                    BottomBarItemAdapter.TYPE_SHARE -> chromeViewModel.share.call()
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.Light)
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
        shoppingSearchResultViewModel.uiModel.observe(this, Observer { uiModel ->
            val tabItems = uiModel.sites.map { site ->
                TabItem(
                    ContentTabFragment.newInstance(site.searchUrl),
                    site.title
                )
            }
            viewPager.adapter = ShoppingSearchTabsAdapter(childFragmentManager, tabItems)
        })
    }

    private fun initTabLayout() {
        tabLayout.setupWithViewPager(viewPager)
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
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()

    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()

    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()

    private fun reload() = sessionManager.focusSession?.engineSession?.reload()
}
