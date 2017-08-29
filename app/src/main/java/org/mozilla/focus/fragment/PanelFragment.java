package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class PanelFragment extends Fragment {

    protected void closePanel() {
        ((ListPanelDialog)getParentFragment()).dismiss();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if( ! (getParentFragment() instanceof ListPanelDialog) ) {
            throw new RuntimeException("PanelFragments needs its parent to be an instance of ListPanelDialog");
        }
    }

}
