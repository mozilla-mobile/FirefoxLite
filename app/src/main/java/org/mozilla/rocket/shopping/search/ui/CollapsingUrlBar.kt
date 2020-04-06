package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewParent
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.layout_collapsing_url_bar.view.*
import org.mozilla.focus.R
import org.mozilla.rocket.extension.dpToPx
import kotlin.math.abs

class CollapsingUrlBar : ConstraintLayout, AppBarLayout.OnOffsetChangedListener {

    private val titleMarginStart: Int
    private val titleMarginStartCollapsed: Int
    private val titleMarginBottomCollapsed: Int
    private var isTitleMinHeightSet = false

    private var titleText: String? = null
    private var iconRedId: Int? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CollapsingUrlBar,
            0,
            0
        ).apply {
            try {
                titleText = getString(R.styleable.CollapsingUrlBar_title)
                iconRedId = getResourceId(R.styleable.CollapsingUrlBar_iconResId, 0).takeIf { it != 0 }
            } finally {
                recycle()
            }
        }
        titleText?.let { setTitle(it) }
        iconRedId?.let { setIcon(it) }
    }

    init {
        titleMarginStart = dpToPx(TITLE_MARGIN_START_IN_DP)
        titleMarginStartCollapsed = dpToPx(TITLE_MARGIN_START_COLLAPSED_IN_DP)
        titleMarginBottomCollapsed = dpToPx(TITLE_MARGIN_BOTTOM_COLLAPSED_IN_DP)
        LayoutInflater.from(context).inflate(R.layout.layout_collapsing_url_bar, this, true)
    }

    fun setTitle(titleText: String) {
        this.titleText = titleText
        title?.text = titleText
    }

    fun setIcon(resId: Int) {
        this.iconRedId = resId
        icon?.setImageResource(resId)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeAppBarLayoutOffsetChangedEvent(parent)
    }

    override fun onDetachedFromWindow() {
        unsubscribeAppBarLayoutOffsetChangedEvent(parent)
        super.onDetachedFromWindow()
    }

    private fun subscribeAppBarLayoutOffsetChangedEvent(parent: ViewParent) {
        if (parent is AppBarLayout) {
            parent.addOnOffsetChangedListener(this)
        }
    }

    private fun unsubscribeAppBarLayoutOffsetChangedEvent(parent: ViewParent) {
        if (parent is AppBarLayout) {
            parent.removeOnOffsetChangedListener(this)
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val progress = abs(verticalOffset / (height - minHeight.toFloat()))
        updateIcon(progress)
        updateTitle(progress)
        updateToolbar(progress)
    }

    private fun updateIcon(progress: Float) {
        val scale = 1 - (1 - ICON_SIZE_RATIO) * progress
        icon.scaleX = scale
        icon.scaleY = scale
        icon.translationX = -(titleMarginStart - titleMarginStartCollapsed) * progress * ICON_TRANSLATION_X_RATIO
        icon.translationY = (toolbar.height - titleMarginBottomCollapsed) * progress * ICON_TRANSLATION_Y_RATIO
    }

    private fun updateTitle(progress: Float) {
        if (!isTitleMinHeightSet) {
            // To avoid view height changing caused by text size updates
            title.minHeight = title.height
            title.gravity = Gravity.BOTTOM
            isTitleMinHeightSet = true
        }
        title.textSize = TITLE_SIZE_IN_SP * (TITLE_SIZE_RATIO + (1 - TITLE_SIZE_RATIO) * (1 - progress))
        title.translationX = -(titleMarginStart - titleMarginStartCollapsed) * progress
        title.translationY = (toolbar.height - titleMarginBottomCollapsed) * progress
    }

    private fun updateToolbar(progress: Float) {
        toolbar.alpha = 1f - progress
    }

    companion object {
        private const val ICON_SIZE_IN_DP = 24f
        private const val ICON_SIZE_COLLAPSED_IN_DP = 14f
        private const val ICON_SIZE_RATIO = ICON_SIZE_COLLAPSED_IN_DP / ICON_SIZE_IN_DP
        private const val ICON_TRANSLATION_X_RATIO = 0.45f
        private const val ICON_TRANSLATION_Y_RATIO = 0.55f

        private const val TITLE_SIZE_IN_SP = 18f
        private const val TITLE_SIZE_COLLAPSED_IN_SP = 12f
        private const val TITLE_SIZE_RATIO = TITLE_SIZE_COLLAPSED_IN_SP / TITLE_SIZE_IN_SP
        private const val TITLE_MARGIN_START_IN_DP = 56f
        private const val TITLE_MARGIN_START_COLLAPSED_IN_DP = 27f
        private const val TITLE_MARGIN_BOTTOM_COLLAPSED_IN_DP = 4f
    }
}