package org.mozilla.rocket.adapter

import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.collection.SparseArrayCompat
import kotlin.reflect.KClass

class AdapterDelegatesManager {
    private val typeDelegateMap = SparseArrayCompat<AdapterDelegate>()
    private val modelTypeMap = ArrayMap<KClass<out DelegateAdapter.UiModel>, Int>()

    fun add(clazz: KClass<out DelegateAdapter.UiModel>, layoutId: Int, delegate: AdapterDelegate) {
        typeDelegateMap.put(layoutId, delegate)
        modelTypeMap[clazz] = layoutId
    }

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegateAdapter.ViewHolder {
        val delegate = typeDelegateMap[viewType]
        requireNotNull(delegate) { "Cannot find delegate with viewType: $viewType" }
        val view = delegate.inflateView(parent, viewType)

        return delegate.onCreateViewHolder(view)
    }

    fun getItemViewType(uiModel: DelegateAdapter.UiModel): Int =
            modelTypeMap[uiModel::class] ?: error("Cannot find viewType with class: ${uiModel.javaClass}")

    fun onBindViewHolder(holder: DelegateAdapter.ViewHolder, uiModel: DelegateAdapter.UiModel) {
        holder.bind(uiModel)
    }
}