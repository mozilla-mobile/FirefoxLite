/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately.home

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.airbnb.lottie.LottieAnimationView
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_private_homescreen.pm_home_brand_description
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.privately.ShortcutUtils
import org.mozilla.rocket.privately.ShortcutViewModel
import org.mozilla.rocket.widget.CustomViewDialogData
import org.mozilla.rocket.widget.PromotionDialog
import javax.inject.Inject

class PrivateHomeFragment : LocaleAwareFragment(),
        ScreenNavigator.HomeScreen {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var logoMan: LottieAnimationView
    private lateinit var fakeInput: View
    private lateinit var privateModeBtn: View

    @Override
    override fun onCreate(bundle: Bundle?) {
        appComponent().inject(this)
        super.onCreate(bundle)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? =
            inflater.inflate(R.layout.fragment_private_homescreen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logoMan = view.findViewById(R.id.pm_home_logo)
        fakeInput = view.findViewById(R.id.pm_home_fake_input)
        initDescription()

        fakeInput.setOnClickListener { chromeViewModel.showUrlInput.call() }
        chromeViewModel.isHomePageUrlInputShowing.observe(viewLifecycleOwner, Observer { isShowing ->
            if (isShowing == true) hideFakeInput() else showFakeInput()
        })
        privateModeBtn = view.findViewById(R.id.pm_home_private_mode_btn)
        privateModeBtn.setOnClickListener {
            chromeViewModel.togglePrivateMode.call()
            TelemetryWrapper.togglePrivateMode(false)
        }
        observeViewModel()
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun initDescription() {
        val highlightStr = getString(R.string.private_home_description_2)
        val descriptionStr = getString(R.string.private_home_description_1, highlightStr)
        val str = SpannableString(descriptionStr).apply {
            val highlightIndex = descriptionStr.indexOf(highlightStr)
            setSpan(ForegroundColorSpan(Color.WHITE), highlightIndex, highlightIndex + highlightStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        pm_home_brand_description.text = str
    }

    private fun observeViewModel() {
        activity?.apply {
            val shortcutViewModel = ViewModelProvider(this).get(ShortcutViewModel::class.java)
            monitorShortcutPromotion(this, shortcutViewModel)
            monitorShortcutMessage(this, shortcutViewModel)
            monitorShortcutCreation(this, shortcutViewModel)
        }
    }

    private fun monitorShortcutPromotion(context: Context, model: ShortcutViewModel) {
        model.eventPromoteShortcut.observe(viewLifecycleOwner, Observer { callback ->
            val data = CustomViewDialogData().apply {
                this.drawable = VectorDrawableCompat.create(resources, R.drawable.dialog_pbshortcut, null)
                this.title = context.getString(R.string.private_browsing_dialog_add_shortcut_title_v2)
                this.description = context.getString(R.string.private_browsing_dialog_add_shortcut_content_v2)
                this.positiveText = context.getString(R.string.private_browsing_dialog_add_shortcut_yes_v2)
                this.negativeText = context.getString(R.string.private_browsing_dialog_add_shortcut_no_v2)
                this.showCloseButton = true
            }

            PromotionDialog(context, data)
                    .onPositive { callback?.onPositive() }
                    .onNegative { callback?.onNegative() }
                    .onClose { callback?.onNegative() }
                    .onCancel { callback?.onCancel() }
                    .setCancellable(false)
                    .show()
        })
    }

    private fun monitorShortcutMessage(context: Context, model: ShortcutViewModel) {
        model.eventShowMessage.observe(viewLifecycleOwner, Observer {
            val msgId = it ?: return@Observer
            Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show()
        })
    }

    private fun monitorShortcutCreation(context: Context, model: ShortcutViewModel) {
        model.eventCreateShortcut.observe(viewLifecycleOwner, Observer {
            ShortcutUtils.createShortcut(context)
        })
    }

    override fun applyLocale() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun create(): PrivateHomeFragment {
            return PrivateHomeFragment()
        }
    }

    private fun animatePrivateHome() {
        privateModeBtn.apply {
            findViewById<LottieAnimationView>(R.id.pm_home_mask).playAnimation()
        }
        logoMan.playAnimation()
    }

    override fun onUrlInputScreenVisible(visible: Boolean) {
        if (visible) {
            chromeViewModel.onShowHomePageUrlInput()
        } else {
            chromeViewModel.onDismissHomePageUrlInput()
        }
    }

    private fun showFakeInput() {
        logoMan.visibility = View.VISIBLE
        fakeInput.visibility = View.VISIBLE
    }

    private fun hideFakeInput() {
        logoMan.visibility = View.INVISIBLE
        fakeInput.visibility = View.INVISIBLE
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (enter) {
            val anim = AnimationUtils.loadAnimation(activity, R.anim.pb_enter)

            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    animatePrivateHome()
                }
            })

            return anim
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim)
        }
    }
}
