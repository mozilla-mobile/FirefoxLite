package org.mozilla.rocket.settings.defaultbrowser.ui

import android.text.Spannable

data class DefaultBrowserTutorialDialogData(
    val title: String,
    val firstStepDescription: Spannable,
    val firstStepImageDefaultResId: Int,
    val firstStepImageUrl: String,
    val firstStepImageWidth: Int,
    val firstStepImageHeight: Int,
    val secondStepDescription: Spannable,
    val secondStepImageDefaultResId: Int,
    val secondStepImageUrl: String,
    val secondStepImageWidth: Int,
    val secondStepImageHeight: Int,
    val positiveText: String,
    val negativeText: String
)
