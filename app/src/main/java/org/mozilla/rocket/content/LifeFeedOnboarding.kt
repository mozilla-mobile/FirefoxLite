package org.mozilla.rocket.content

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.SupportUtils

object LifeFeedOnboarding {

    private val ONBOARDING_VERSION = 1
    private const val PREF_KEY_INT_LIFE_FEED_ONBOARDING_VERSION = "pref_key_int_life_feed_onboarding_version"

    @JvmStatic
    fun shouldShow(context: Context): Boolean {
        val featureNotice = NewFeatureNotice.getInstance(context)
        val shouldShowNews = shouldShowOnboarding(context) && AppConfigWrapper.hasNewsPortal()
        val shouldShowShoppingLink = featureNotice.shouldShowEcShoppingLinkOnboarding()
        return shouldShowNews || shouldShowShoppingLink
    }

    @JvmStatic
    fun hasShown(context: Context) {
        NewFeatureNotice.getInstance(context).hasShownEcShoppingLink()
        hasShownOnboarding(context)
    }

    @JvmStatic
    fun getContentText(context: Context): Spannable {
        val lifeFeed = context.getString(R.string.life_feed)
        val body = context.getString(R.string.first_run_page6_text, lifeFeed, "%s")
        val learnMore = context.getString(R.string.about_link_learn_more)
        val feedURL = SupportUtils.getSumoURLForTopic(context, "firefox-lite-feed")

        val content = String.format(body, learnMore)
        val start = content.indexOf(learnMore)
        val end = start + learnMore.length
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                context.startActivity(InfoActivity.getIntentFor(context, feedURL, lifeFeed))
            }
        }
        val linkSpan = SpannableStringBuilder(content)
        linkSpan.setSpan(clickableSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return linkSpan
    }

    private fun shouldShowOnboarding(context: Context): Boolean {
        val currentOnBoardingVersion = getSharedPreferences(context).getInt(PREF_KEY_INT_LIFE_FEED_ONBOARDING_VERSION, 0)
        return currentOnBoardingVersion < ONBOARDING_VERSION
    }

    private fun hasShownOnboarding(context: Context) {
        getSharedPreferences(context).edit().putInt(PREF_KEY_INT_LIFE_FEED_ONBOARDING_VERSION, ONBOARDING_VERSION).apply()
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}