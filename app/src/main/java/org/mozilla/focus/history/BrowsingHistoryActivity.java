package org.mozilla.focus.history;

import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import org.mozilla.focus.R;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class BrowsingHistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnClearHistory;

    @SuppressFBWarnings(value = {"DLS"}, justification = "Needed for future versions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browsing_history);

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.browsing_history_view_root);

        View bottomSheet = coordinatorLayout.findViewById(R.id.browsing_history_btm_sheet_container);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        mBtnClearHistory = (Button) findViewById(R.id.browsing_history_btn_clear);
        mBtnClearHistory.setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) coordinatorLayout.findViewById(R.id.browsing_history_recycler_view);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                break;
        }
    }
}
