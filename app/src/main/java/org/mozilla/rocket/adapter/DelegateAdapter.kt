package org.mozilla.rocket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

class DelegateAdapter(
    private val delegatesManager: AdapterDelegatesManager
) : RecyclerView.Adapter<DelegateAdapter.ViewHolder>() {

    private var data = mutableListOf<UIModel>()

    fun setData(data: List<UIModel>) {
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
        abstract fun bind(UIModel: UIModel)
    }

    open class UIModel
}

interface AdapterDelegate {
    fun inflateView(parent: ViewGroup, layoutId: Int): View =
            LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

    fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder

    fun onBindViewHolder(UIModel: DelegateAdapter.UIModel, position: Int, holder: DelegateAdapter.ViewHolder) {
        holder.bind(UIModel)
    }
}