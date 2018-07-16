package org.mozilla.rocket.privately.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE.TOGGLE_PRIVATE_MODE
import org.mozilla.rocket.widget.BetterBounceInterpolator

class PrivateHomeFragment : LocaleAwareFragment() {

    lateinit var btnBack: View
    lateinit var logoMan: View;

    @Override
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle);
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_private_homescreen, container, false)
        btnBack = view.findViewById(R.id.btn_tab_tray)
        logoMan = view.findViewById(R.id.logo_man)

        btnBack.setOnClickListener { v ->
            // TODO: we should use Navigator
            val listener = activity as FragmentListener
            listener.onNotified(PrivateHomeFragment@ this, TOGGLE_PRIVATE_MODE, null)
        }
        return view
    }

    @Override
    override fun onResume() {
        super.onResume()

        applyAnimation()

    }

    override fun applyLocale() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun applyAnimation() {
        val animationSet = AnimationSet(true)
                .apply { addAnimation(ScaleAnimation(2.0f, 1.0f, 2.0f, 1.0f)) }
                .apply { addAnimation(AlphaAnimation(0.66f, 1.0f)) }
                .apply {
                    val translate = TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.5f,
                            Animation.RELATIVE_TO_SELF, 0.0f,
                            Animation.RELATIVE_TO_SELF, -0.5f,
                            Animation.RELATIVE_TO_SELF, 0.0f)
                    addAnimation(translate)
                }
                .also { set ->
                    set.interpolator = BetterBounceInterpolator(1, -0.8)
                    set.duration = 720
                }

        btnBack.startAnimation(animationSet)
        logoMan.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.pb_logoman))
    }

    companion object {
        const val FRAGMENT_TAG = "private_home_screen"

        fun create(): PrivateHomeFragment {
            return PrivateHomeFragment()
        }
    }
}