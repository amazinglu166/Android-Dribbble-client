package com.example.amazinglu.my_dribbble.bucket_list;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.amazinglu.my_dribbble.R;
import com.example.amazinglu.my_dribbble.auth_request.DribbbleFunc;
import com.example.amazinglu.my_dribbble.base.DribbbleException;
import com.example.amazinglu.my_dribbble.base.DribbbleTask;
import com.example.amazinglu.my_dribbble.base.SpaceItemdecoration;
import com.example.amazinglu.my_dribbble.model.Bucket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by AmazingLu on 11/10/17.
 */

public class BucketListFragment extends Fragment {

    public static final int REQ_CODE_NEW_BUCKET = 100;

    @BindView(R.id.bucket_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.fab) FloatingActionButton floatingActionButton;
    @BindView(R.id.bucket_refresh) SwipeRefreshLayout swipeRefreshLayout;

    private BuckListAdapter adapter;

    public static BucketListFragment newInstance() {
        return new BucketListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bucket_recycler_view, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        swipeRefreshLayout.setEnabled(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new SpaceItemdecoration(
                getResources().getDimensionPixelSize(R.dimen.spacing_medium)));
        /**
         * initialize the adapter
         * */
        adapter = new BuckListAdapter(new ArrayList<Bucket>(), new BuckListAdapter.LoadMoreListener() {
            @Override
            public void onLoadMore() {
                AsyncTaskCompat.executeParallel(new LoadBucketTask(false));
            }
        });
        recyclerView.setAdapter(adapter);

        /**
         * click the add bucket button
         * */
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * go to the dialog fragment
                 * */
                NewBucketDialogFragment dialogFragment = NewBucketDialogFragment.newInstance();
                dialogFragment.setTargetFragment(BucketListFragment.this, REQ_CODE_NEW_BUCKET);
                dialogFragment.show(getFragmentManager(), NewBucketDialogFragment.TAG);
            }
        });

        /**
         * click the refresh
         * */
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                AsyncTaskCompat.executeParallel(new LoadBucketTask(true));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_NEW_BUCKET && resultCode == Activity.RESULT_OK) {
            String bucketName = data.getStringExtra(NewBucketDialogFragment.KEY_BUCKET_NAME);
            String bucketDescription = data.getStringExtra(NewBucketDialogFragment.KEY_BUCKET_DESCRIPTION);
            if (!TextUtils.isEmpty(bucketName)) {
                AsyncTaskCompat.executeParallel(new NewBucketTask(bucketName, bucketDescription));
            }
        }
    }

    private class LoadBucketTask extends DribbbleTask<Void, Void, List<Bucket>> {

        private int page;
        private boolean refresh;

        public LoadBucketTask(boolean refresh) {
            this.refresh = refresh;
        }

        @Override
        protected List<Bucket> doJob(Void... params) throws DribbbleException, IOException {
            page = refresh ? 1 : adapter.getDataCount() / DribbbleFunc.COUNT_PER_PAGE + 1;
            return DribbbleFunc.getUserBucket(page);
        }

        @Override
        protected void onSuccess(List<Bucket> buckets) {
            /**
             * no matter refresh or load more data
             * we need to check the showloading
             * */
            adapter.setShowLoading(buckets.size() == DribbbleFunc.COUNT_PER_PAGE);

            if (refresh) {
                adapter.setData(buckets);
                swipeRefreshLayout.setRefreshing(false);
                refresh = false;
            } else {
                swipeRefreshLayout.setEnabled(true);
                adapter.append(buckets);
            }
        }

        @Override
        protected void onFailed(DribbbleException e) {
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private class NewBucketTask extends DribbbleTask<Void, Void, Bucket> {

        private String bucketName;
        private String bucketDescription;

        public NewBucketTask(String bucketName, String bucketDescription) {
            this.bucketName = bucketName;
            this.bucketDescription = bucketDescription;
        }

        @Override
        protected Bucket doJob(Void... params) throws DribbbleException, IOException {
            return DribbbleFunc.newBucket(bucketName, bucketDescription);
        }

        @Override
        protected void onSuccess(Bucket bucket) {
            adapter.prepend(Collections.singletonList(bucket));
        }

        @Override
        protected void onFailed(DribbbleException e) {
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }


}
