package org.mozilla.rocket.content

import android.content.Context
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.mozilla.focus.FocusApplication
import org.mozilla.rocket.di.AppComponent
import org.mozilla.rocket.extension.toFragmentActivity

fun Fragment.app(): FocusApplication = context?.applicationContext as FocusApplication
fun FragmentActivity.app(): FocusApplication = applicationContext as FocusApplication
fun AppCompatDialog.app(): FocusApplication = context.applicationContext as FocusApplication
fun Fragment.appComponent(): AppComponent = app().appComponent
fun FragmentActivity.appComponent(): AppComponent = app().appComponent
fun AppCompatDialog.appComponent(): AppComponent = app().appComponent
fun Fragment.appContext(): Context = appComponent().appContext()
fun FragmentActivity.appContext(): Context = appComponent().appContext()

/**
 * Like [FragmentActivity.viewModelProvider] for FragmentActivity that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> FragmentActivity.viewModelProvider(
    provider: ViewModelProvider.Factory
) =
    ViewModelProviders.of(this, provider).get(VM::class.java)

/**
 * Like [Fragment.viewModelProvider] for Fragments that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> Fragment.activityViewModelProvider(
    provider: ViewModelProvider.Factory
) =
    ViewModelProviders.of(requireActivity(), provider).get(VM::class.java)

/**
 * Like [AppCompatDialog.viewModelProvider] for AppCompatDialog that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified VM : ViewModel> AppCompatDialog.activityViewModelProvider(
    provider: ViewModelProvider.Factory
) =
    ViewModelProviders.of(context.toFragmentActivity(), provider).get(VM::class.java)
