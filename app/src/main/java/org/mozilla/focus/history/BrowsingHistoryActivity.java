package org.mozilla.focus.history;

import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;

import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public class BrowsingHistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnClearHistory;
    private ViewGroup mContainerRecyclerView;
    private ViewGroup mContainerEmptyView;
    private HistoryItemAdapter mAdapter;

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

        mContainerRecyclerView = (ViewGroup) findViewById(R.id.browsing_history_recycler_view);
        mContainerEmptyView = (ViewGroup) findViewById(R.id.browsing_history_empty_view_container);

        RecyclerView recyclerView = (RecyclerView) coordinatorLayout.findViewById(R.id.browsing_history_recycler_view);
        mAdapter = new HistoryItemAdapter(recyclerView, this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        ArrayList<Site> sites = new ArrayList<>();
//        sites.add(new Site("Amazon", "https://www.amazon.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Yahoo", "https://www.yahoo.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Amazon", "https://www.amazon.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Yahoo", "https://www.yahoo.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Amazon", "https://www.amazon.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Yahoo", "https://www.yahoo.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Amazon", "https://www.amazon.com/", Calendar.getInstance().getTimeInMillis()));
//        sites.add(new Site("Yahoo", "https://www.yahoo.com/", Calendar.getInstance().getTimeInMillis()));
//        updateSites(sites);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                updateSites(null);
                break;
            default:
                break;
        }
    }

    private void updateSites(ArrayList<Site> sites) {
        if(sites == null || sites.size() == 0) {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.VISIBLE);

        } else{
            mContainerRecyclerView.setVisibility(View.VISIBLE);
            mContainerEmptyView.setVisibility(View.GONE);
        }
        mAdapter.updateSites(sites);
    }
}
