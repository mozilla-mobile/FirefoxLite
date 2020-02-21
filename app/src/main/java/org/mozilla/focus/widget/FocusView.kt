package org.mozilla.focus.widget

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils

class FocusView : RelativeLayout {
    private val transparentPaint: Paint = Paint()
    private val path = Path()
    private var centerX: Int = 0
    private var centerY: Int = 0
    private var statusBarOffset = 0
    private var radius: Int = 0
    private var backgroundDimColor: Int = 0

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initPaints()
    }

    /** FocusView will draw a spotlight circle(total transparent) at coordinates X = centerX and Y = centerY with radius.
     * The view's background except the spotlight circle is half-transparent. */
    constructor(context: Context, centerX: Int, centerY: Int, radius: Int, backgroundColor: Int) : super(context) {
        this.centerX = centerX
        this.centerY = centerY
        this.statusBarOffset = ViewUtils.getStatusBarHeight(context as Activity)
        this.radius = radius
        this.backgroundDimColor = backgroundColor

        initPaints()
        drawSpotlight()
        val left = this.centerX - radius
        val top = this.centerY - statusBarOffset - radius
        addSpotlightPlaceholder(radius * 2, radius * 2, left, top)
    }

    private fun initPaints() {
        transparentPaint.color = Color.TRANSPARENT
        transparentPaint.strokeWidth = 10f
    }

    private fun drawSpotlight() {
        addView(object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)

                path.reset()

                path.addCircle(centerX.toFloat(), (centerY - statusBarOffset).toFloat(), radius.toFloat(), Path.Direction.CW)
                path.fillType = Path.FillType.INVERSE_EVEN_ODD

                canvas.drawCircle(centerX.toFloat(), (centerY - statusBarOffset).toFloat(), radius.toFloat(), transparentPaint)
                canvas.clipPath(path)
                canvas.drawColor(backgroundDimColor)
            }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun addSpotlightPlaceholder(width: Int, height: Int, left: Int, top: Int) {
        addView(View(context).apply {
            id = R.id.spotlight_anchor_view
        }, LayoutParams(width, height).apply {
            leftMargin = left
            topMargin = top
        })
        addView(View(context).apply {
            id = R.id.spotlight_placeholder
        }, LayoutParams(width, height).apply {
            addRule(ALIGN_TOP, R.id.spotlight_anchor_view)
            addRule(ALIGN_START, R.id.spotlight_anchor_view)
        })
    }
}
