package org.mozilla.focus.widget

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.utils.ViewUtils

class RoundRecFocusView : View {
    private val transparentPaint: Paint = Paint()
    private val path = Path()
    private var centerX: Int = 0
    private var centerY: Int = 0
    private var statusBarOffset = 0
    private var radius: Int = 0
    private var rectangleHeight: Int = 0
    private var rectangleWidth: Int = 0
    private var backgroundDimColor = 0
    private lateinit var rectF: RectF

    constructor(context: Context) : super(context) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initPaints()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initPaints()
    }

    constructor(context: Context, centerX: Int, centerY: Int, offsetY: Int, radius: Int, height: Int, width: Int, backgroundColor: Int) : super(context) {
        this.centerX = centerX
        this.centerY = centerY - offsetY
        this.statusBarOffset = ViewUtils.getStatusBarHeight(context as Activity)
        this.radius = radius
        this.rectangleHeight = height
        this.rectangleWidth = width
        this.backgroundDimColor = backgroundColor

        val left = centerX - rectangleWidth / 2
        val top = this.centerY - rectangleHeight / 2 - statusBarOffset
        val right = centerX + rectangleWidth / 2
        val bottom = this.centerY + rectangleHeight / 2 - statusBarOffset
        this.rectF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        initPaints()
    }

    private fun initPaints() {
        transparentPaint.color = Color.TRANSPARENT
        transparentPaint.strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()
        path.addRoundRect(rectF, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
        path.fillType = Path.FillType.INVERSE_EVEN_ODD

        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), transparentPaint)
        canvas.clipPath(path)
        canvas.drawColor(backgroundDimColor)
    }
}
