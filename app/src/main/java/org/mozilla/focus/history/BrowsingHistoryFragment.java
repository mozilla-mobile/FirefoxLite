package org.mozilla.focus.history;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.mozilla.focus.R;


public class BrowsingHistoryFragment extends Fragment implements View.OnClickListener, HistoryItemAdapter.EmptyListener {

    private Button mBtnClearHistory;
    private RecyclerView mContainerRecyclerView;
    private ViewGroup mContainerEmptyView;
    private HistoryItemAdapter mAdapter;

    public static BrowsingHistoryFragment newInstance() {
        return new BrowsingHistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browsing_history, container, false);
        mBtnClearHistory = (Button) v.findViewById(R.id.browsing_history_btn_clear);
        mBtnClearHistory.setOnClickListener(this);

        mContainerRecyclerView = (RecyclerView) v.findViewById(R.id.browsing_history_recycler_view);
        mContainerEmptyView = (ViewGroup) v.findViewById(R.id.browsing_history_empty_view_container);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new HistoryItemAdapter(mContainerRecyclerView, getActivity(), this, layoutManager);
        mContainerRecyclerView.setAdapter(mAdapter);
        mContainerRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                mAdapter.clear();
                break;
            default:
                break;
        }
    }

    @Override
    public void onEmpty(boolean flag) {
        if(flag) {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.VISIBLE);

        } else{
            mContainerRecyclerView.setVisibility(View.VISIBLE);
            mContainerEmptyView.setVisibility(View.GONE);
        }
    }
}
