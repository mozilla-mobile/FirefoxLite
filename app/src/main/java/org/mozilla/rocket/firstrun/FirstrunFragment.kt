/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.firstrun

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_first_run.animation_description
import kotlinx.android.synthetic.main.fragment_first_run.animation_layout
import kotlinx.android.synthetic.main.fragment_first_run.animation_view
import kotlinx.android.synthetic.main.fragment_first_run.description
import kotlinx.android.synthetic.main.fragment_first_run.item_browsing
import kotlinx.android.synthetic.main.fragment_first_run.item_games
import kotlinx.android.synthetic.main.fragment_first_run.item_news
import kotlinx.android.synthetic.main.fragment_first_run.item_shopping
import kotlinx.android.synthetic.main.fragment_first_run.progress_bar
import kotlinx.android.synthetic.main.fragment_first_run.select_button
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.appContext
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.firstrun.FirstrunFragment.ContentPrefItem.Browsing
import org.mozilla.rocket.firstrun.FirstrunFragment.ContentPrefItem.Games
import org.mozilla.rocket.firstrun.FirstrunFragment.ContentPrefItem.News
import org.mozilla.rocket.firstrun.FirstrunFragment.ContentPrefItem.Shopping
import org.mozilla.rocket.home.data.ContentPrefRepo
import org.mozilla.rocket.home.domain.SetContentPrefUseCase
import org.mozilla.rocket.periodic.FirstLaunchWorker
import org.mozilla.rocket.periodic.PeriodicReceiver
import javax.inject.Inject

class FirstrunFragment : Fragment(), ScreenNavigator.FirstrunScreen {

    @Inject
    lateinit var firstrunViewModelCreator: Lazy<FirstrunViewModel>

    private lateinit var firstrunViewModel: FirstrunViewModel

    private var currentSelectedItem: ContentPrefItem? = null

    private var pageStartTime = 0L

    override fun getFragment(): Fragment {
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        firstrunViewModel = getViewModel(firstrunViewModelCreator)
        TelemetryWrapper.showFirstRunOnBoarding()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_first_run, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        description.text = getString(R.string.firstrun_fxlite_2_5_title_B, getString(R.string.app_name))
        select_button.setOnClickListener { goNext() }
        initContentPrefItems()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        returnTransition = TransitionInflater.from(context).inflateTransition(R.transition.firstrun_exit)
    }

    override fun onResume() {
        super.onResume()
        pageStartTime = System.currentTimeMillis()
    }

    private fun goNext() {
        NewFeatureNotice.getInstance(context).setFirstRunDidShow()
        NewFeatureNotice.getInstance(context).setLiteUpdateDidShow()
        activity?.sendBroadcast(Intent(activity, PeriodicReceiver::class.java).apply {
            action = FirstLaunchWorker.ACTION
        })

        currentSelectedItem.let { selectedItem ->
            requireNotNull(selectedItem)
            sendItemSelectedTelemetry(selectedItem, System.currentTimeMillis() - pageStartTime)
            SetContentPrefUseCase(ContentPrefRepo(appContext())).invoke(selectedItem.toContentPref())
        }

        showAnimation()
    }

    private fun sendItemSelectedTelemetry(selectedItem: ContentPrefItem, timeSpent: Long) {
        TelemetryWrapper.clickFirstRunOnBoarding(
            timeSpent,
            0,
            true,
            when (selectedItem) {
                Browsing -> TelemetryWrapper.Extra_Value.CONTENT_PREF_DEFAULT
                Shopping -> TelemetryWrapper.Extra_Value.CONTENT_PREF_DEALS
                Games -> TelemetryWrapper.Extra_Value.CONTENT_PREF_ENTERTAINMENT
                News -> TelemetryWrapper.Extra_Value.CONTENT_PREF_NEWS
            }
        )
    }

    private fun showAnimation() {
        animation_layout.isVisible = true

        var textIndex = 0
        var nextText = TEXT_SHOWING_LIST[0]

        val animationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator: ValueAnimator ->
                val progress = valueAnimator.animatedValue as Float
                animation_view?.progress = progress

                val time = (progress * ANIMATION_DURATION).toInt()
                if (time >= nextText.first) {
                    animation_description?.text = getString(nextText.second)
                    nextText = if (++textIndex < TEXT_SHOWING_LIST.size) {
                        TEXT_SHOWING_LIST[textIndex]
                    } else {
                        Int.MAX_VALUE to 0
                    }
                }
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    (activity as? MainActivity)?.firstrunFinished()
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })
        }
        progress_bar.max = PROGRESS_BAR_MAX
        val progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PROGRESS_BAR_DURATION
            startDelay = PROGRESS_BAR_START_DELAY
            interpolator = AccelerateInterpolator(PROGRESS_BAR_ACCELERATE_FACTOR)
            addUpdateListener { valueAnimator: ValueAnimator ->
                val progress = valueAnimator.animatedValue as Float
                progress_bar?.progress = (progress * PROGRESS_BAR_MAX).toInt()
            }
        }

        val fadeInFadeOutAnimation = AnimationSet(false).apply {
            addAnimation(AlphaAnimation(0f, 1f).apply {
                interpolator = DecelerateInterpolator()
                startOffset = DESCRIPTION_FADE_IN_OFFSET
                duration = DESCRIPTION_FADE_IN_FADE_OUT_DURATION
            })
            addAnimation(AlphaAnimation(1f, 0f).apply {
                interpolator = AccelerateInterpolator()
                startOffset = DESCRIPTION_FADE_OUT_OFFSET
                duration = DESCRIPTION_FADE_IN_FADE_OUT_DURATION
            })
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    progress_bar.visibility = View.INVISIBLE
                    animation_description.visibility = View.INVISIBLE
                }

                override fun onAnimationStart(animation: Animation?) {
                    progress_bar.visibility = View.VISIBLE
                    animation_description.visibility = View.VISIBLE
                }
            })
        }

        AnimatorSet().apply {
            playTogether(animationAnimator, progressAnimator)
        }.start()
        progress_bar.animation = fadeInFadeOutAnimation
        animation_description.animation = fadeInFadeOutAnimation
    }

    override fun isAnimationRunning(): Boolean = animation_layout?.isVisible == true

    private fun initContentPrefItems() {
        setContentPrefSelected(Browsing)

        item_browsing.setOnClickListener { setContentPrefSelected(Browsing) }
        item_shopping.setOnClickListener { setContentPrefSelected(Shopping) }
        item_games.setOnClickListener { setContentPrefSelected(Games) }
        item_news.setOnClickListener { setContentPrefSelected(News) }
    }

    private fun setContentPrefSelected(item: ContentPrefItem) {
        if (currentSelectedItem == item) return

        currentSelectedItem = item
        listOf(Browsing, Shopping, Games, News).groupBy { it == item }.run {
            get(true)?.forEach { view?.setContentPrefSelected(it, true) }
            get(false)?.forEach { view?.setContentPrefSelected(it, false) }
        }
    }

    private fun View.setContentPrefSelected(item: ContentPrefItem, selected: Boolean) {
        val view = this.findViewById<View>(item.viewId)
        val icon = view.findViewById<View>(item.iconId)
        val textView = view.findViewById<TextView>(item.textId)
        view.isSelected = selected
        icon.isVisible = selected
        val textColorId = if (selected) {
            R.color.paletteWhite100
        } else {
            R.color.paletteDarkGreyC100
        }
        textView.setTextColor(ContextCompat.getColor(context, textColorId))
    }

    private sealed class ContentPrefItem(val viewId: Int, val textId: Int, val iconId: Int) {
        object Browsing : ContentPrefItem(R.id.item_browsing, R.id.text_browsing, R.id.icon_browsing)
        object Shopping : ContentPrefItem(R.id.item_shopping, R.id.text_shopping, R.id.icon_shopping)
        object Games : ContentPrefItem(R.id.item_games, R.id.text_games, R.id.icon_games)
        object News : ContentPrefItem(R.id.item_news, R.id.text_news, R.id.icon_news)
    }

    private fun ContentPrefItem.toContentPref(): ContentPrefRepo.ContentPref {
        return when (this) {
            Browsing -> ContentPrefRepo.ContentPref.Browsing
            Shopping -> ContentPrefRepo.ContentPref.Shopping
            Games -> ContentPrefRepo.ContentPref.Games
            News -> ContentPrefRepo.ContentPref.News
        }
    }

    companion object {
        private const val ANIMATION_DURATION = 5940L
        private const val PROGRESS_BAR_DURATION = 5280L
        private const val PROGRESS_BAR_START_DELAY = 165L
        private const val PROGRESS_BAR_MAX = 1000
        private const val PROGRESS_BAR_ACCELERATE_FACTOR = 2f
        private const val DESCRIPTION_FADE_IN_FADE_OUT_DURATION = 165L
        private const val DESCRIPTION_FADE_IN_OFFSET = 165L
        private const val DESCRIPTION_FADE_OUT_OFFSET = 5610L

        private val TEXT_SHOWING_LIST = listOf(
            165 to R.string.onboarding_transition_feature_1,
            2310 to R.string.onboarding_transition_feature_2,
            3795 to R.string.onboarding_transition_feature_3
        )

        @JvmStatic
        fun create(): FirstrunFragment {
            return FirstrunFragment()
        }
    }
}
