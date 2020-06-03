package org.mozilla.rocket.content

import android.content.Context
import android.preference.Preference
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Lazy
import org.mozilla.focus.FocusApplication
import org.mozilla.rocket.di.AppComponent
import org.mozilla.rocket.extension.toFragmentActivity

fun Fragment.app(): FocusApplication = context?.applicationContext as FocusApplication
fun FragmentActivity.app(): FocusApplication = applicationContext as FocusApplication
fun AppCompatDialog.app(): FocusApplication = context.applicationContext as FocusApplication
fun Preference.app(): FocusApplication = context.applicationContext as FocusApplication
fun Fragment.appComponent(): AppComponent = app().getAppComponent()
fun FragmentActivity.appComponent(): AppComponent = app().getAppComponent()
fun AppCompatDialog.appComponent(): AppComponent = app().getAppComponent()
fun Preference.appComponent(): AppComponent = app().getAppComponent()
fun Fragment.appContext(): Context = appComponent().appContext()

@VisibleForTesting
fun Context.appComponent(): AppComponent = (applicationContext as FocusApplication).getAppComponent()

/**
 * Like [Fragment.viewModelProvider] for Fragment that want a [ViewModel] scoped to the Fragment.
 */
inline fun <reified T : ViewModel> Fragment.getViewModel(creator: Lazy<T>? = null): T {
    return if (creator == null)
        ViewModelProvider(this).get(T::class.java)
    else
        ViewModelProvider(this, BaseViewModelFactory { creator.get() }).get(T::class.java)
}

/**
 * Like [FragmentActivity.viewModelProvider] for FragmentActivity that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified T : ViewModel> FragmentActivity.getViewModel(creator: Lazy<T>? = null): T {
    return if (creator == null)
        ViewModelProvider(this).get(T::class.java)
    else
        ViewModelProvider(this, BaseViewModelFactory { creator.get() }).get(T::class.java)
}

/**
 * Like [Fragment.viewModelProvider] for Fragments that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified T : ViewModel> Fragment.getActivityViewModel(creator: Lazy<T>? = null): T {
    return if (creator == null)
        ViewModelProvider(requireActivity()).get(T::class.java)
    else
        ViewModelProvider(requireActivity(), BaseViewModelFactory { creator.get() }).get(T::class.java)
}

/**
 * Like [AppCompatDialog.viewModelProvider] for AppCompatDialog that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified T : ViewModel> AppCompatDialog.getActivityViewModel(creator: Lazy<T>? = null): T {
    return if (creator == null)
        ViewModelProvider(context.toFragmentActivity()).get(T::class.java)
    else
        ViewModelProvider(context.toFragmentActivity(), BaseViewModelFactory { creator.get() }).get(T::class.java)
}

/**
 * Like [Preference.viewModelProvider] for Preference that want a [ViewModel] scoped to the Activity.
 */
inline fun <reified T : ViewModel> Preference.getActivityViewModel(creator: Lazy<T>? = null): T {
    return if (creator == null)
        ViewModelProvider(context.toFragmentActivity()).get(T::class.java)
    else
        ViewModelProvider(context.toFragmentActivity(), BaseViewModelFactory { creator.get() }).get(T::class.java)
}