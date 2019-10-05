package org.mozilla.rocket.fxa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.rocket.content.common.ui.ContentTabFragment

class FxAccountFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_fx_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                    .replace(R.id.browser_container, ContentTabFragment.newInstance(BuildConfig.FXA_SETTINGS_URL, false))
                    .commit()
        }
    }
}