package org.mozilla.rocket.settings.defaultbrowser.ui

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.layout_default_browser_tutorial_dialog.view.first_step_description
import kotlinx.android.synthetic.main.layout_default_browser_tutorial_dialog.view.first_step_image
import kotlinx.android.synthetic.main.layout_default_browser_tutorial_dialog.view.second_step_description
import kotlinx.android.synthetic.main.layout_default_browser_tutorial_dialog.view.second_step_image
import kotlinx.android.synthetic.main.layout_default_browser_tutorial_dialog.view.title
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp

class DefaultBrowserTutorialDialog(
    private val context: Context,
    private val data: DefaultBrowserTutorialDialogData
) {
    val view: View = View.inflate(context, R.layout.layout_default_browser_tutorial_dialog, null)

    private var onPositiveListener: (() -> Unit)? = null
    private var onNegativeListener: (() -> Unit)? = null
    private var onCloseListener: (() -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    private val onShowListeners = mutableListOf<() -> Unit>()
    private val onDismissListeners = mutableListOf<() -> Unit>()

    private var cancellable = false

    init {
        initView()
    }

    fun onPositive(listener: () -> Unit): DefaultBrowserTutorialDialog {
        this.onPositiveListener = listener
        return this
    }

    fun onNegative(listener: () -> Unit): DefaultBrowserTutorialDialog {
        this.onNegativeListener = listener
        return this
    }

    fun onClose(listener: () -> Unit): DefaultBrowserTutorialDialog {
        this.onCloseListener = listener
        return this
    }

    fun onCancel(listener: () -> Unit): DefaultBrowserTutorialDialog {
        this.onCancelListener = listener
        return this
    }

    fun addOnShowListener(listener: () -> Unit): DefaultBrowserTutorialDialog {
        onShowListeners.add(listener)
        return this
    }

    fun addOnDismissListener(listener: () -> Unit): DefaultBrowserTutorialDialog {
        onDismissListeners.add(listener)
        return this
    }

    fun setCancellable(cancellable: Boolean): DefaultBrowserTutorialDialog {
        this.cancellable = cancellable
        return this
    }

    @Suppress("DEPRECATION")
    fun show() {
        val dialog = createDialog()
        dialog.show()

        val buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val buttonNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        if (buttonPositive != null && buttonNegative != null) {
            buttonPositive.setTextAppearance(context, R.style.TutorialDialogPositiveButtonStyle)
            buttonNegative.setTextAppearance(context, R.style.TutorialDialogNegativeButtonStyle)
        }
    }

    private fun initView() {
        with(view.title) {
            data.title.let { text = it }
        }

        with(view.first_step_description) {
            data.firstStepDescription.let { text = it }
        }

        with(view.first_step_image) {
            val width = data.firstStepImageWidth
            val height = data.firstStepImageHeight
            if (width != 0 && height != 0) {
                layoutParams.apply {
                    this.width = width
                    this.height = height
                }
            }

            when {
                data.firstStepImageUrl.isNotEmpty() -> {
                    GlideApp.with(context)
                        .asBitmap()
                        .placeholder(data.firstStepImageDefaultResId)
                        .load(data.firstStepImageUrl)
                        .into(this)
                }
                data.firstStepImageDefaultResId != 0 -> {
                    setImageResource(data.firstStepImageDefaultResId)
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }

        with(view.second_step_description) {
            data.secondStepDescription.let { text = it }
        }

        with(view.second_step_image) {
            val width = data.secondStepImageWidth
            val height = data.secondStepImageHeight
            if (width != 0 && height != 0) {
                layoutParams.apply {
                    this.width = width
                    this.height = height
                }
            }

            when {
                data.secondStepImageUrl.isNotEmpty() -> {
                    GlideApp.with(context)
                        .asBitmap()
                        .placeholder(data.secondStepImageDefaultResId)
                        .load(data.secondStepImageUrl)
                        .into(this)
                }
                data.secondStepImageDefaultResId != 0 -> {
                    setImageResource(data.secondStepImageDefaultResId)
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun createDialog(): AlertDialog {
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setOnCancelListener {
                onCancelListener?.invoke()
            }
            .setCancelable(cancellable)
            .setPositiveButton(data.positiveText) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                onPositiveListener?.invoke()
            }
            .setNegativeButton(data.negativeText) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                onNegativeListener?.invoke()
            }
            .create()

        dialog.setOnShowListener {
            onShowListeners.forEach { it() }
        }

        dialog.setOnDismissListener {
            onDismissListeners.forEach { it() }
        }

        return dialog
    }
}