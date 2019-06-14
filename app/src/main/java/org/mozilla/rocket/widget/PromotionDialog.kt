package org.mozilla.rocket.widget

import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.view.View
import kotlinx.android.synthetic.main.layout_promotion_dialog.view.*
import org.mozilla.focus.R

class PromotionDialog(
    private val context: Context,
    private val data: CustomViewDialogData
) {
    private var view: View = View.inflate(context, R.layout.layout_promotion_dialog, null)

    private var onPositiveListener: (() -> Unit)? = null
    private var onNegativeListener: (() -> Unit)? = null
    private var onCloseListener: (() -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    private var cancellable = false

    init {
        initView()
    }

    fun onPositive(listener: () -> Unit): PromotionDialog {
        this.onPositiveListener = listener
        return this
    }

    fun onNegative(listener: () -> Unit): PromotionDialog {
        this.onNegativeListener = listener
        return this
    }

    fun onClose(listener: () -> Unit): PromotionDialog {
        this.onCloseListener = listener
        return this
    }

    fun onCancel(listener: () -> Unit): PromotionDialog {
        this.onCancelListener = listener
        return this
    }

    fun setCancellable(cancellable: Boolean): PromotionDialog {
        this.cancellable = cancellable
        return this
    }

    fun show() {
        createDialog().show()
    }

    private fun initView() {
        with(view.image) {
            data.drawable?.let { setImageDrawable(it) } ?: run { visibility = View.GONE }
        }

        with(view.title) {
            data.title?.let { text = it } ?: run { visibility = View.GONE }
        }

        with(view.description) {
            data.description?.let { text = it } ?: run { visibility = View.GONE }
        }

        with(view.positive_button) {
            data.positiveText?.let { text = it } ?: run {
                visibility = View.GONE
                view.button_divider1.visibility = View.GONE
            }
        }

        with(view.negative_button) {
            data.negativeText?.let { text = it } ?: run {
                visibility = View.GONE
                view.button_divider2.visibility = View.GONE
            }
        }

        view.close_button.visibility = if (data.showCloseButton) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun createDialog(): AlertDialog {
        val dialog = AlertDialog.Builder(context)
                .setView(view)
                .setOnCancelListener {
                    onCancelListener?.invoke()
                }
                .setCancelable(cancellable)
                .create()

        view.positive_button.setOnClickListener {
            dialog.dismiss()
            onPositiveListener?.invoke()
        }

        view.negative_button.setOnClickListener {
            dialog.dismiss()
            onNegativeListener?.invoke()
        }

        view.close_button.setOnClickListener {
            dialog.dismiss()
            onCloseListener?.invoke()
        }

        return dialog
    }
}
