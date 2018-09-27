package org.mozilla.focus.widget

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils

class FocusView : View {
    private val transparentPaint: Paint = Paint()
    private val path = Path()
    private var centerX: Int = 0
    private var centerY: Int = 0
    private var statusBarOffset = 0
    private var radius: Int = 0

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
    constructor(context: Context, centerX: Int, centerY: Int, radius: Int) : super(context) {
        this.centerX = centerX
        this.centerY = centerY
        this.statusBarOffset = ViewUtils.getStatusBarHeight(context as Activity)
        this.radius = radius
        initPaints()
    }

    private fun initPaints() {
        transparentPaint.color = Color.TRANSPARENT
        transparentPaint.strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        path.reset()

        path.addCircle(centerX.toFloat(), (centerY - statusBarOffset).toFloat(), radius.toFloat(), Path.Direction.CW)
        path.fillType = Path.FillType.INVERSE_EVEN_ODD

        canvas.drawCircle(centerX.toFloat(), (centerY - statusBarOffset).toFloat(), radius.toFloat(), transparentPaint)
        canvas.clipPath(path)
        canvas.drawColor(ContextCompat.getColor(context, R.color.myShotOnBoardingBackground))
    }
}
