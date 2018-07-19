package org.mozilla.rocket.privately.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE.SHOW_URL_INPUT
import org.mozilla.focus.widget.FragmentListener.TYPE.TOGGLE_PRIVATE_MODE


class PrivateHomeFragment : LocaleAwareFragment() {

    private lateinit var btnBack: View
    private lateinit var logoMan: View
    private lateinit var fakeInput: View

    @Override
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle);
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_private_homescreen, container, false)
        btnBack = view.findViewById(R.id.btn_tab_tray)
        logoMan = view.findViewById(R.id.logo_man)
        fakeInput = view.findViewById(R.id.home_fragment_fake_input)

        btnBack.setOnClickListener {
            var parent = activity
            if (parent is FragmentListener) {
                parent.onNotified(PrivateHomeFragment@ this, TOGGLE_PRIVATE_MODE, null)
            }
        }

        fakeInput.setOnClickListener {
            var parent = activity
            if (parent is FragmentListener) {
                parent.onNotified(PrivateHomeFragment@ this, SHOW_URL_INPUT, null)
            }
        }

        return view
    }

    @Override
    override fun onResume() {
        super.onResume()


    }

    override fun applyLocale() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    companion object {
        const val FRAGMENT_TAG = "private_home_screen"

        fun create(): PrivateHomeFragment {
            return PrivateHomeFragment()
        }
    }
}