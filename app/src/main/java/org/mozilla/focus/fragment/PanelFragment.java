package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class PanelFragment extends Fragment {

    protected void closePanel() {
        ((ListPanelDialog)getTargetFragment()).dismiss();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if( ! (getTargetFragment() instanceof ListPanelDialog) ) {
            throw new RuntimeException("PanelFragments should have its Target Fragment set to an instance of ListPanelDialog");
        }
    }

}
