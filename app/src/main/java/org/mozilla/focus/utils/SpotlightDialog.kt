package org.mozilla.focus.utils

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout.LayoutParams
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import org.mozilla.focus.R
import org.mozilla.focus.widget.FocusView
import org.mozilla.focus.widget.RoundRecFocusView
import org.mozilla.rocket.extension.dpToPx

class SpotlightDialog private constructor(
    private val activity: Activity,
    private val targetView: View,
    private val configs: SpotlightConfigs,
    private val attachedView: View?,
    private val attachedViewConfigs: AttachedViewConfigs?,
    private val additionalViews: List<Pair<View, ViewGroup.LayoutParams>>,
    private val cancelOnTouchOutside: Boolean,
    private var dismissListener: DialogInterface.OnDismissListener?,
    private var cancelListener: DialogInterface.OnCancelListener?
) {

    private fun create(): Dialog {
        val focusView = (getFocusView(activity, targetView, configs) as ViewGroup).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addAdditionalViews(this, additionalViews)
            addAttachedView(this, attachedView, attachedViewConfigs)
        }

        return AlertDialog.Builder(activity, R.style.TabTrayTheme)
                .setView(focusView)
                .create()
                .apply {
                    setOnDismissListener(dismissListener)
                    setOnCancelListener(cancelListener)
                }
                .also { dialog ->
                    if (cancelOnTouchOutside) {
                        focusView.setOnClickListener { dialog.dismiss() }
                        focusView.findViewById<View>(R.id.spotlight_placeholder).apply {
                            setOnClickListener {
                                dialog.dismiss()
                                targetView.performClick()
                            }
                            setOnLongClickListener {
                                dialog.dismiss()
                                targetView.performLongClick()
                            }
                        }
                    }
                }
    }

    private fun getFocusView(activity: Activity, targetView: View, spotlightConfigs: SpotlightConfigs): View {
        val (targetX, targetY) = IntArray(2).run {
            targetView.getLocationInWindow(this)
            this[0] to this[1]
        }

        val offsetX = (configs.xOffsetRatio * targetView.measuredWidth).toInt()
        val offsetY = (configs.yOffsetRatio * targetView.measuredHeight).toInt()
        val centerX = targetX + targetView.measuredWidth / 2 + offsetX
        val centerY = targetY + targetView.measuredHeight / 2 + offsetY

        return when (configs) {
            is SpotlightConfigs.CircleSpotlightConfigs -> {
                // Set default radius
                val radius = if (configs.radius > 0) {
                    configs.radius
                } else {
                    maxOf(targetView.measuredWidth, targetView.measuredHeight) / 2
                }

                FocusView(activity, centerX, centerY, radius, spotlightConfigs.backgroundDimColor)
            }
            is SpotlightConfigs.RectangleSpotlightConfigs -> {
                val spotlightWidth = if (configs.width > 0) {
                    configs.width
                } else {
                    (configs.widthRatio * targetView.measuredWidth).toInt()
                }
                val spotlightHeight = if (configs.height > 0) {
                    configs.height
                } else {
                    (configs.heightRatio * targetView.measuredHeight).toInt()
                }
                // Set default radius
                val cornerRadius = configs.cornerRadius ?: targetView.dpToPx(DEFAULT_CORNER_RADIUS_IN_DP)

                RoundRecFocusView(activity, centerX, centerY, cornerRadius, spotlightHeight, spotlightWidth, spotlightConfigs.backgroundDimColor)
            }
        }
    }

    private fun addAdditionalViews(parent: ViewGroup, pairs: List<Pair<View, ViewGroup.LayoutParams>>) {
        pairs.forEach { (view, layoutParams) ->
            parent.addView(view, layoutParams)
        }
    }

    private fun addAttachedView(parent: ViewGroup, attachedView: View?, attachedViewConfigs: AttachedViewConfigs?) {
        if (attachedView != null) {
            requireNotNull(attachedViewConfigs) { "Must to have AttachedViewConfigs" }

            val layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                when (attachedViewConfigs.position) {
                    AttachedPosition.LEFT, AttachedPosition.RIGHT -> {
                        if (attachedViewConfigs.position == AttachedPosition.LEFT) {
                            addRule(RelativeLayout.LEFT_OF, R.id.spotlight_placeholder)
                        } else {
                            addRule(RelativeLayout.RIGHT_OF, R.id.spotlight_placeholder)
                        }
                        when (attachedViewConfigs.gravity) {
                            AttachedGravity.START -> addRule(RelativeLayout.ALIGN_TOP, R.id.spotlight_placeholder)
                            AttachedGravity.END -> addRule(RelativeLayout.ALIGN_BOTTOM, R.id.spotlight_placeholder)
                            AttachedGravity.START_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_TOP)
                            AttachedGravity.CENTER_SCREEN -> addRule(RelativeLayout.CENTER_VERTICAL)
                            AttachedGravity.END_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                            AttachedGravity.END_ALIGN_START -> addRule(RelativeLayout.ABOVE, R.id.spotlight_placeholder)
                            AttachedGravity.START_ALIGN_END -> addRule(RelativeLayout.BELOW, R.id.spotlight_placeholder)
                        }
                    }
                    AttachedPosition.TOP, AttachedPosition.BOTTOM -> {
                        if (attachedViewConfigs.position == AttachedPosition.TOP) {
                            addRule(RelativeLayout.ABOVE, R.id.spotlight_placeholder)
                        } else {
                            addRule(RelativeLayout.BELOW, R.id.spotlight_placeholder)
                        }
                        when (attachedViewConfigs.gravity) {
                            AttachedGravity.START -> addRule(RelativeLayout.ALIGN_START, R.id.spotlight_placeholder)
                            AttachedGravity.END -> addRule(RelativeLayout.ALIGN_END, R.id.spotlight_placeholder)
                            AttachedGravity.START_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_START)
                            AttachedGravity.CENTER_SCREEN -> addRule(RelativeLayout.CENTER_HORIZONTAL)
                            AttachedGravity.END_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_END)
                            AttachedGravity.END_ALIGN_START -> addRule(RelativeLayout.LEFT_OF, R.id.spotlight_placeholder)
                            AttachedGravity.START_ALIGN_END -> addRule(RelativeLayout.RIGHT_OF, R.id.spotlight_placeholder)
                        }
                    }
                }

                topMargin = attachedViewConfigs.marginTop
                bottomMargin = attachedViewConfigs.marginBottom
                leftMargin = attachedViewConfigs.marginStart
                rightMargin = attachedViewConfigs.marginEnd
            }

            parent.addView(attachedView, layoutParams)
        }
    }

    data class Builder(
        private val activity: Activity,
        private val targetView: View
    ) {
        private var configs: SpotlightConfigs = SpotlightConfigs.RectangleSpotlightConfigs()
        private var attachedView: View? = null
        private var attachedViewConfigs: AttachedViewConfigs? = null
        private val additionalViews: MutableList<Pair<View, ViewGroup.LayoutParams>> = arrayListOf()
        private var cancelOnTouchOutside: Boolean = true
        private var dismissListener: DialogInterface.OnDismissListener? = null
        private var cancelListener: DialogInterface.OnCancelListener? = null

        fun spotlightConfigs(configs: SpotlightConfigs) = apply {
            this.configs = configs
        }

        fun addView(
            view: View,
            layoutParams: ViewGroup.LayoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        ) = apply {
            additionalViews.add(view to layoutParams)
        }

        fun setAttachedView(view: View, configs: AttachedViewConfigs) = apply {
            this.attachedView = view
            this.attachedViewConfigs = configs
        }

        fun cancelOnTouchOutside(cancelOnTouchOutside: Boolean) = apply {
            this.cancelOnTouchOutside = cancelOnTouchOutside
        }

        fun dismissListener(dismissListener: DialogInterface.OnDismissListener?) = apply {
            this.dismissListener = dismissListener
        }

        fun cancelListener(cancelListener: DialogInterface.OnCancelListener?) = apply {
            this.cancelListener = cancelListener
        }

        fun build(): Dialog = SpotlightDialog(
            activity,
            targetView,
            configs,
            attachedView,
            attachedViewConfigs,
            additionalViews,
            cancelOnTouchOutside,
            dismissListener,
            cancelListener
        ).create()
    }

    sealed class SpotlightConfigs(
        open val backgroundDimColor: Int,
        open val xOffsetRatio: Float,
        open val yOffsetRatio: Float
    ) {
        data class CircleSpotlightConfigs(
            override val backgroundDimColor: Int = DEFAULT_BG_COLOR,
            override val xOffsetRatio: Float = 0f,
            override val yOffsetRatio: Float = 0f,
            val radius: Int = Int.MIN_VALUE
        ) : SpotlightConfigs(backgroundDimColor, xOffsetRatio, yOffsetRatio)

        data class RectangleSpotlightConfigs(
            override val backgroundDimColor: Int = DEFAULT_BG_COLOR,
            override val xOffsetRatio: Float = 0f,
            override val yOffsetRatio: Float = 0f,
            val width: Int = Int.MIN_VALUE,
            val height: Int = Int.MIN_VALUE,
            val widthRatio: Float = 1.0f,
            val heightRatio: Float = 1.0f,
            val cornerRadius: Int? = null
        ) : SpotlightConfigs(backgroundDimColor, xOffsetRatio, yOffsetRatio)
    }

    data class AttachedViewConfigs(
        val position: AttachedPosition,
        val gravity: AttachedGravity = AttachedGravity.START,
        val marginStart: Int = 0,
        val marginTop: Int = 0,
        val marginEnd: Int = 0,
        val marginBottom: Int = 0
    )

    enum class AttachedGravity {
        START, END_ALIGN_START, END, START_ALIGN_END, START_SCREEN, CENTER_SCREEN, END_SCREEN
    }

    enum class AttachedPosition {
        LEFT, TOP, RIGHT, BOTTOM
    }

    companion object {
        private const val DEFAULT_CORNER_RADIUS_IN_DP = 8f
        private const val DEFAULT_BG_COLOR = 0x80000000.toInt()
    }
}