package org.mozilla.rocket.content

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.mozilla.focus.FocusApplication
import org.mozilla.rocket.di.AppComponent

fun Fragment.app(): FocusApplication = context?.applicationContext as FocusApplication
fun FragmentActivity.app(): FocusApplication = applicationContext as FocusApplication
fun Fragment.appComponent(): AppComponent = app().appComponent
fun FragmentActivity.appComponent(): AppComponent = app().appComponent
fun Fragment.appContext(): Context = appComponent().appContext()
fun FragmentActivity.appContext(): Context = appComponent().appContext()

/**
 * Like [Fragment.viewModelProvider] for Fragments that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> Fragment.activityViewModelProvider(
    provider: ViewModelProvider.Factory
) =
    ViewModelProviders.of(requireActivity(), provider).get(VM::class.java)
