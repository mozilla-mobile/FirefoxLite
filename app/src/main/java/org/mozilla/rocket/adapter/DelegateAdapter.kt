package org.mozilla.rocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

open class DelegateAdapter(
    private val delegatesManager: AdapterDelegatesManager
) : RecyclerView.Adapter<DelegateAdapter.ViewHolder>() {

    internal var data = mutableListOf<UiModel>()

    fun setData(data: List<UiModel>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int): Int =
            delegatesManager.getItemViewType(data[position])

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        delegatesManager.onBindViewHolder(holder, data[position])
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view), LayoutContainer {
        abstract fun bind(uiModel: UiModel)
    }

    abstract class UiModel
}

interface AdapterDelegate {
    fun inflateView(parent: ViewGroup, layoutId: Int): View =
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

    fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder

    fun onBindViewHolder(uiModel: DelegateAdapter.UiModel, position: Int, holder: DelegateAdapter.ViewHolder) {
        holder.bind(uiModel)
    }
}