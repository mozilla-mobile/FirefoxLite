package org.mozilla.rocket.content.news.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_news.news_list
import kotlinx.android.synthetic.main.fragment_news.news_progress_center
import kotlinx.android.synthetic.main.fragment_news.no_result_view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.adapter.DelegatePagedListAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.ContentTabActivity
import org.mozilla.rocket.content.common.ui.VerticalTelemetryViewModel
import org.mozilla.rocket.content.common.ui.firstImpression
import org.mozilla.rocket.content.common.ui.monitorScrollImpression
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.ui.adapter.NewsAdapterDelegate
import org.mozilla.rocket.content.news.ui.adapter.NewsSourceLogoAdapterDelegate
import org.mozilla.rocket.content.news.ui.adapter.NewsSourceLogoUiModel
import org.mozilla.rocket.content.news.ui.adapter.NewsUiModel
import javax.inject.Inject

class NewsFragment : Fragment() {

    @Inject
    lateinit var applicationContext: Context

    @Inject
    lateinit var newsViewModelCreator: Lazy<NewsViewModel>

    @Inject
    lateinit var telemetryViewModelCreator: Lazy<VerticalTelemetryViewModel>

    private lateinit var newsViewModel: NewsViewModel
    private lateinit var telemetryViewModel: VerticalTelemetryViewModel
    private lateinit var newsAdapter: DelegatePagedListAdapter

    private var isLoading = false
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var stateLoadingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        newsViewModel = getActivityViewModel(newsViewModelCreator)
        telemetryViewModel = getActivityViewModel(telemetryViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNewsList()
        bindNewsListData()
        handleActions()
    }

    private fun initNewsList() {
        newsAdapter = object : DelegatePagedListAdapter(
            AdapterDelegatesManager().apply {
                add(NewsSourceLogoUiModel::class, R.layout.item_news_source_logo, NewsSourceLogoAdapterDelegate())
                add(NewsUiModel::class, R.layout.item_news, NewsAdapterDelegate(getCategory(), newsViewModel))
            },
            object : DiffUtil.ItemCallback<DelegateAdapter.UiModel>() {
                override fun areItemsTheSame(oldItem: DelegateAdapter.UiModel, newItem: DelegateAdapter.UiModel): Boolean {
                    if (oldItem::class != newItem::class) {
                        return false
                    }
                    return when (oldItem) {
                        is NewsSourceLogoUiModel -> true
                        is NewsUiModel -> {
                            newItem as NewsUiModel
                            oldItem.componentId == newItem.componentId
                        }
                        else -> error("unexpected type: ${oldItem.javaClass}")
                    }
                }

                override fun areContentsTheSame(oldItem: DelegateAdapter.UiModel, newItem: DelegateAdapter.UiModel): Boolean {
                    return when (oldItem) {
                        is NewsSourceLogoUiModel -> true
                        is NewsUiModel -> {
                            newItem as NewsUiModel == oldItem
                        }
                        else -> error("unexpected type: ${oldItem.javaClass}")
                    }
                }
            }
        ) {
            override fun getItemId(position: Int): Long {
                return when (val item = getItem(position)) {
                    is NewsSourceLogoUiModel -> item.hashCode().toLong()
                    is NewsUiModel -> item.componentId.hashCode().toLong()
                    null -> super.getItemId(position)
                    else -> error("unexpected type: ${item.javaClass}")
                }
            }
        }.apply {
            setHasStableIds(true)
        }

        news_list.apply {
            adapter = newsAdapter
            monitorScrollImpression(telemetryViewModel)
        }
    }

    private fun bindNewsListData() {
        val newsLiveData: LiveData<PagedList<DelegateAdapter.UiModel>> =
            newsViewModel.startToObserveNews(getCategory(), getLanguage())
        newsLiveData.observe(viewLifecycleOwner, Observer { items ->
            onStatus(items)
            newsAdapter.submitList(items)
            telemetryViewModel.updateVersionId(getCategory(), newsViewModel.versionId)
            isLoading = false

            news_list.firstImpression(
                telemetryViewModel,
                getCategory(),
                NewsItem.DEFAULT_SUB_CATEGORY_ID
            )

            telemetryViewModel.impression.observe(viewLifecycleOwner, Observer { impression ->
                if (getCategory() == impression.category) {
                    newsViewModel.onNewsItemsShown(impression)
                }
            })
        })
    }

    private fun handleActions() {
        newsViewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is NewsViewModel.NewsAction.OpenLink -> {
                    context?.let {
                        startActivity(ContentTabActivity.getStartIntent(it, event.url, event.telemetryData, enableTurboMode = false))
                    }
                }
            }
        })

        no_result_view.setButtonOnClickListener(View.OnClickListener {
            // call onStatus() again with null to display the loading indicator
            onStatus(null)
            newsViewModel.remove(getCategory())
            bindNewsListData()
        })
    }

    fun onStatus(items: List<DelegateAdapter.UiModel>?) {
        when {
            items == null -> {
                stateLoading()
                stateLoadingJob = uiScope.launch {
                    delay(TIME_MILLIS_STATE_LOADING_THRESHOLD)
                    stateEmpty()
                }
            }
            items.isEmpty() -> {
                stateLoadingJob?.cancel()
                stateLoadingJob = null
                stateEmpty()
            }
            else -> {
                stateLoadingJob?.cancel()
                stateLoadingJob = null
                stateDisplay()
            }
        }
    }

    private fun stateDisplay() {
        news_list.isVisible = true
        no_result_view.isVisible = false
        news_progress_center.isVisible = false
    }

    private fun stateEmpty() {
        news_list.isVisible = false
        no_result_view.isVisible = true
        news_progress_center.isVisible = false
    }

    private fun stateLoading() {
        news_list.isVisible = false
        no_result_view.isVisible = false
        news_progress_center.isVisible = true
    }

    private fun getCategory(): String {
        return arguments?.getString(NewsTabFragment.TYPE_KEY) ?: "top-news"
    }

    private fun getLanguage(): String {
        return arguments?.getString(NewsTabFragment.EXTRA_NEWS_LANGUAGE) ?: "english"
    }

    companion object {
        private const val TIME_MILLIS_STATE_LOADING_THRESHOLD = 5000L

        fun newInstance(category: String, language: String): NewsFragment {
            val args = Bundle().apply {
                putString(NewsTabFragment.TYPE_KEY, category)
                putString(NewsTabFragment.EXTRA_NEWS_LANGUAGE, language)
            }
            return NewsFragment().apply {
                arguments = args
            }
        }
    }
}
