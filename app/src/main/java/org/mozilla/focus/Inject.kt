/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.app.Application
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.animation.Animation
import org.mozilla.focus.home.HomeFragment
import org.mozilla.focus.persistence.TabsDatabase
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.RemoteConfigConstants
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.download.DownloadInfoRepository
import org.mozilla.rocket.download.DownloadInfoViewModel
import org.mozilla.rocket.download.DownloadViewModelFactory
import org.mozilla.rocket.urlinput.GlobalDataSource
import org.mozilla.rocket.urlinput.LocaleDataSource
import org.mozilla.rocket.urlinput.QuickSearchRepository
import org.mozilla.rocket.urlinput.QuickSearchViewModel
import org.mozilla.rocket.urlinput.QuickSearchViewModelFactory
import org.mozilla.strictmodeviolator.StrictModeViolation

object Inject {

    @JvmStatic
    var activityNewlyCreatedFlag = true
        private set

    @JvmStatic
    val isUnderEspressoTest: Boolean
        get() = false

    val defaultFeatureSurvey: RemoteConfigConstants.SURVEY
        get() = RemoteConfigConstants.SURVEY.NONE

    @JvmStatic
    fun getDefaultTopSites(context: Context): String? {

        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(HomeFragment.TOPSITES_PREF, null)
    }

    @JvmStatic
    fun getTabsDatabase(context: Context): TabsDatabase {
        return TabsDatabase.getInstance(context)
    }

    fun isTelemetryEnabled(context: Context): Boolean {
        // The first access to shared preferences will require a disk read.
        return StrictModeViolation.tempGrant({ obj: Builder -> obj.permitDiskReads() }, {
            val resources = context.resources
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isEnabledByDefault = AppConstants.isBuiltWithFirebase()
            // Telemetry is not enable by default in debug build. But the user / developer can choose to turn it on
            // in AndroidTest, this is enabled by default
            preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), isEnabledByDefault)
        })
    }

    fun enableStrictMode() {
        if (AppConstants.isReleaseBuild()) {
            return
        }

        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll()

        threadPolicyBuilder.penaltyLog().penaltyDialog()
        // Previously we have penaltyDeath() for debug build, but in order to add crashlytics, we can't use it here.
        // ( crashlytics has untagged Network violation so it always crashes
        vmPolicyBuilder.penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    @JvmStatic
    fun setActivityNewlyCreatedFlag() {
        activityNewlyCreatedFlag = false
    }

    fun provideDownloadInfoRepository(): DownloadInfoRepository {
        // TODO inject data source, ex production DB or mock DB here
        return DownloadInfoRepository.getInstance()
    }

    @JvmStatic
    fun obtainDownloadIndicatorViewModel(activity: FragmentActivity): DownloadIndicatorViewModel {
        val factory = DownloadViewModelFactory.getInstance()
        return ViewModelProviders.of(activity, factory).get(DownloadIndicatorViewModel::class.java)
    }

    @JvmStatic
    fun obtainDownloadInfoViewModel(activity: FragmentActivity): DownloadInfoViewModel {
        val factory = DownloadViewModelFactory.getInstance()
        return ViewModelProviders.of(activity, factory).get(DownloadInfoViewModel::class.java)
    }

    private fun provideQuickSearchRepository(application: Application): QuickSearchRepository? {
        return QuickSearchRepository.getInstance(
            GlobalDataSource.getInstance(application)!!,
            LocaleDataSource.getInstance(application)!!
        )
    }

    fun obtainQuickSearchViewModel(activity: FragmentActivity): QuickSearchViewModel {
        val factory = QuickSearchViewModelFactory(provideQuickSearchRepository(activity.application)!!)
        return ViewModelProviders.of(activity, factory).get(QuickSearchViewModel::class.java)
    }

    @JvmStatic
    fun startAnimation(view: View?, animation: Animation) {
        if (view == null) {
            return
        }
        view.startAnimation(animation)
    }
}
