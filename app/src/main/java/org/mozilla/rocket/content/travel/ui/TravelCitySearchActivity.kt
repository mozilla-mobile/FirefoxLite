package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_search_city.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.CitySearchResultUiModel
import javax.inject.Inject

class TravelCitySearchActivity : AppCompatActivity() {

    @Inject
    lateinit var searchViewModelCreator: Lazy<TravelCitySearchViewModel>
    private lateinit var searchViewModel: TravelCitySearchViewModel
    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        searchViewModel = getViewModel(searchViewModelCreator)
        setContentView(R.layout.activity_search_city)
        search_keyword_edit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchViewModel.search(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        search_keyword_edit.post {
            search_keyword_edit.requestFocus()
        }
        clear.setOnClickListener {
            search_keyword_edit.setText("")
        }
        initCityList()
    }

    private fun initCityList() {
        recyclerView.let {
            it.layoutManager = LinearLayoutManager(this@TravelCitySearchActivity)
            adapter = DelegateAdapter(AdapterDelegatesManager().apply {
                add(CitySearchResultUiModel::class, R.layout.item_city_search_result, CitySearchResultAdapterDelegate(searchViewModel))
            })
            it.adapter = adapter
        }
        searchViewModel.items.observe(this, Observer {
            if (it != null) {
                adapter.setData(it)
            }
        })
        searchViewModel.openCity.observe(this, Observer {
        })
        searchViewModel.changeClearBtnVisibility.observe(this, Observer {
            if (it != null) {
                clear.visibility = it
            }
        })
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, TravelCitySearchActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}