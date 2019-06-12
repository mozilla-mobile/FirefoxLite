/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.privately.ShortcutUtils
import org.mozilla.rocket.privately.ShortcutViewModel
import org.mozilla.rocket.widget.PromotionDialog
import org.mozilla.rocket.widget.CustomViewDialogData

class PrivateHomeFragment : LocaleAwareFragment(),
        ScreenNavigator.HomeScreen {

    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var logoMan: LottieAnimationView
    private lateinit var fakeInput: View
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    @Override
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        chromeViewModel = Inject.obtainChromeViewModel(activity)
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_private_homescreen, container, false)
        logoMan = view.findViewById(R.id.pm_home_logo)
        fakeInput = view.findViewById(R.id.pm_home_fake_input)

        fakeInput.setOnClickListener { chromeViewModel.showUrlInput.call() }
        chromeViewModel.isHomePageUrlInputShowing.observe(this, Observer { isShowing ->
            if (isShowing == true) hideFakeInput() else showFakeInput()
        })
        setupBottomBar(view)
        observeViewModel()

        return view
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun setupBottomBar(rootView: View) {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.bottom_bar)
        // Hard code to only show the first item in private home page
        bottomBar.setItemVisibility(1, View.INVISIBLE)
        bottomBar.setItemVisibility(2, View.INVISIBLE)
        bottomBar.setItemVisibility(3, View.INVISIBLE)
        bottomBar.setItemVisibility(4, View.INVISIBLE)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                when (type) {
                    BottomBarItemAdapter.TYPE_PRIVATE_HOME -> {
                        chromeViewModel.togglePrivateMode.call()
                        TelemetryWrapper.togglePrivateMode(false)
                    }
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                }
            }
        })
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.PrivateMode)
        val bottomBarViewModel = Inject.obtainPrivateBottomBarViewModel(activity)
        bottomBarViewModel.items.nonNullObserve(this, bottomBarItemAdapter::setItems)
    }

    private fun observeViewModel() {
        activity?.apply {
            val shortcutViewModel = ViewModelProviders.of(this).get(ShortcutViewModel::class.java)
            monitorShortcutPromotion(this, shortcutViewModel)
            monitorShortcutMessage(this, shortcutViewModel)
            monitorShortcutCreation(this, shortcutViewModel)
        }
    }

    private fun monitorShortcutPromotion(context: Context, model: ShortcutViewModel) {
        model.eventPromoteShortcut.observe(viewLifecycleOwner, Observer { callback ->
            val data = CustomViewDialogData().apply {
                this.drawable = ContextCompat.getDrawable(context, R.drawable.dialog_pbshortcut)
                this.title = context.getString(R.string.private_browsing_dialog_add_shortcut_title)
                this.description = context.getString(R.string.private_browsing_dialog_add_shortcut_content)
                this.positiveText = context.getString(R.string.private_browsing_dialog_add_shortcut_yes)
                this.negativeText = context.getString(R.string.private_browsing_dialog_add_shortcut_no)
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
        bottomBarItemAdapter.animatePrivateHome()
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
