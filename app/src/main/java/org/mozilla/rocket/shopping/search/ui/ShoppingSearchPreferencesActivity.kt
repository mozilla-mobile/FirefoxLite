package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_shopping_search_preferences.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.shopping.search.ui.adapter.ItemMoveCallback
import org.mozilla.rocket.shopping.search.ui.adapter.PreferencesAdapterDelegate
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem
import javax.inject.Inject

class ShoppingSearchPreferencesActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchPreferencesViewModel>

    private lateinit var viewModel: ShoppingSearchPreferencesViewModel

    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_search_preferences)
        viewModel = getViewModel(viewModelCreator)
        initToolBar()
        initPreferenceList()
    }

    override fun onStop() {
        viewModel.onExitSettings()
        super.onStop()
    }

    private fun initToolBar() {
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun initPreferenceList() {
        val adapterDelegate = PreferencesAdapterDelegate(viewModel)
        val adapterDelegatesManager = AdapterDelegatesManager().apply {
            add(ShoppingSiteItem::class, R.layout.item_shopping_search_preference, adapterDelegate)
        }
        adapter = object : DelegateAdapter(adapterDelegatesManager) {
            override fun getItemId(position: Int): Long {
                val uiModel = data[position]
                uiModel as ShoppingSiteItem
                return uiModel.title.hashCode().toLong()
            }
        }.apply {
            setHasStableIds(true)
        }
        recyclerView.apply {
            val itemMoveCallback = ItemMoveCallback(adapterDelegate)
            val touchHelper = ItemTouchHelper(itemMoveCallback)
            touchHelper.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(this@ShoppingSearchPreferencesActivity)
            adapter = this@ShoppingSearchPreferencesActivity.adapter
        }
        viewModel.shoppingSites.observe(this, Observer {
            adapter.setData(it)
        })
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, ShoppingSearchPreferencesActivity::class.java)
    }
}