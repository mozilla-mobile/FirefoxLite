package org.mozilla.rocket.content.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import kotlinx.android.synthetic.main.no_result_view.view.*
import org.mozilla.focus.R

class NoResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val button: Button

    init {
        View.inflate(context, R.layout.no_result_view, this)
        button = findViewById(R.id.no_result_view_button)

        isClickable = false
    }

    fun setIconResource(@DrawableRes resourceId: Int) {
        no_result_view_image.setImageResource(resourceId)
    }

    fun setMessage(message: String) {
        no_result_view_text.text = message
    }

    fun setButtonText(text: String) {
        no_result_view_button.text = text
    }

    fun release() {
        button.setOnClickListener(null)
    }

    fun setButtonOnClickListener(listener: OnClickListener?) {
        button.setOnClickListener(listener)
    }
}
