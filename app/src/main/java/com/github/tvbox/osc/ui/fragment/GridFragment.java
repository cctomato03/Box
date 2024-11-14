package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.GsonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.dialog.GridFilterDialog;
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Stack;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class GridFragment extends BaseLazyFragment {
    private MovieSort.SortData sortData = null;
    private RecyclerView mGridView;
    private SourceViewModel sourceViewModel;
    private GridFilterDialog gridFilterDialog;
    private GridAdapter gridAdapter;
    private int page = 1;
    private int maxPage = 1;
    private boolean isTop = true;

    public static GridFragment newInstance(MovieSort.SortData sortData) {
        return new GridFragment().setArguments(sortData);
    }

    public GridFragment setArguments(MovieSort.SortData sortData) {
        this.sortData = sortData;
        return this;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && this.sortData == null) {
            this.sortData = GsonUtils.fromJson(savedInstanceState.getString("sortDataJson"), MovieSort.SortData.class);
        }
    }

    @Override
    protected void init() {
        try {
            initView();
            initViewModel();
            initData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sortDataJson", GsonUtils.toJson(sortData));        
    }

    // 是否允许聚合搜索 sortData.flag的第二个字符为‘1’时允许聚搜
    public boolean enableFastSearch() {  return sortData.flag == null || sortData.flag.length() < 2 || (sortData.flag.charAt(1) == '1'); }
    //public boolean enableFastSearch() {  return (sortData.flag == null || sortData.flag.length() < 2) ? true : (sortData.flag.charAt(1) == '1'); }

    private void initView() {
        mGridView = findViewById(R.id.mGridView);
        this.gridAdapter = new GridAdapter();
        mGridView.setAdapter(gridAdapter);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        gridAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                gridAdapter.setEnableLoadMore(true);
                sourceViewModel.getList(sortData, page);
            }
        }, mGridView);
        gridAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = gridAdapter.getData().get(position);
                if (video != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    bundle.putString("title", video.name);

                    if (video.id == null || video.id.isEmpty() || video.id.startsWith("msearch:")) {
                        if(Hawk.get(HawkConfig.FAST_SEARCH_MODE, false) && enableFastSearch()){
                            jumpActivity(FastSearchActivity.class, bundle);
                        }else {
                            jumpActivity(SearchActivity.class, bundle);
                        }
                    } else {
                        jumpActivity(DetailActivity.class, bundle);
                    }

                }
            }
        });
        // takagen99 : Long Press to Fast Search
        gridAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = gridAdapter.getData().get(position);
                if (video != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    bundle.putString("title", video.name);
                    jumpActivity(FastSearchActivity.class, bundle);
                }
                return true;
            }
        });
        gridAdapter.setLoadMoreView(new LoadMoreView());
    }

    private void initViewModel() {
        if (sourceViewModel != null) {
            return;
        }
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.listResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                    if (page == 1) {
                        gridAdapter.setNewData(absXml.movie.videoList);
                    } else {
                        gridAdapter.addData(absXml.movie.videoList);
                    }
                    page++;
                    maxPage = absXml.movie.pagecount;
                    if (page > maxPage && maxPage!=0) {
                        gridAdapter.loadMoreEnd();
                        gridAdapter.setEnableLoadMore(false);
                    } else {
                        gridAdapter.loadMoreComplete();
                        gridAdapter.setEnableLoadMore(true);
                    }
                } else {
                    if (page == 1) {
                        showEmpty();
                    }
                    if (page > maxPage && maxPage!=0) {
                        Toast.makeText(getContext(), "没有更多了", Toast.LENGTH_SHORT).show();
                        gridAdapter.loadMoreEnd();
                    } else {
                        gridAdapter.loadMoreComplete();
                    }
                    gridAdapter.setEnableLoadMore(false);
                }
            }
        });
    }

    private void initData() {
    	if (ApiConfig.get().getHomeSourceBean().getApi()==null) {
            showEmpty();
            return;
        }
        scrollTop();
        toggleFilterStatus();
        sourceViewModel.getList(sortData, page);
    }

    private void toggleFilterStatus() {
        if (sortData.filters != null && !sortData.filters.isEmpty()) {
            int count = sortData.filterSelectCount();
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_FILTER_CHANGE, count));
        }
    }

    public boolean isTop() {
        return isTop;
    }

    public void scrollTop() {
        isTop = true;
        mGridView.scrollToPosition(0);
    }
}