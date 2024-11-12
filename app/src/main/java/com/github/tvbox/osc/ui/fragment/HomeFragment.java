package com.github.tvbox.osc.ui.fragment;

import static android.content.Intent.getIntent;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.databinding.FragmentHomeBinding;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeFragment extends Fragment {
    private LinearLayout topLayout;
    private LinearLayout contentLayout;
    private MaterialToolbar toolbar;
    private LoadService mLoadService;
    private TvRecyclerView mGridView;
    private NoScrollViewPager mViewPager;
    private SourceViewModel sourceViewModel;
    private SortAdapter sortAdapter;
    private HomePageAdapter pageAdapter;
    private FragmentHomeBinding binding;
    boolean useCacheConfig = false;
    private final List<BaseLazyFragment> fragments = new ArrayList<>();
    private int currentSelected = 0;
    private final Handler mHandler = new Handler();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        useCacheConfig = false;
        initViewModel();
        initData();
    }

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    private boolean dataInitOk = false;
    private boolean jarInitOk = false;
    boolean HomeShow = Hawk.get(HawkConfig.HOME_SHOW_SOURCE, false);

    private void initData() {
        Hawk.put(HawkConfig.API_URL, "https://file.cctomato.com/tv/1.json");
        // takagen99 : Switch to show / hide source title
//        Hawk.put(HawkConfig.API_URL, "https://xiaoya.cctomato.com/tvbox/my.json");

        SourceBean home = ApiConfig.get().getHomeSourceBean();
        if (HomeShow) {
            if (home != null && home.getName() != null && !home.getName().isEmpty())
                toolbar.setTitle(home.getName());
//                tvName.setText(home.getName());
        }

        if (dataInitOk && jarInitOk) {
            showLoading();
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            return;
        }
        showLoading();
        if (dataInitOk && !jarInitOk) {
            if (!ApiConfig.get().getSpider().isEmpty()) {
                ApiConfig.get().loadJar(useCacheConfig, ApiConfig.get().getSpider(), new ApiConfig.LoadConfigCallback() {
                    @Override
                    public void success() {
                        jarInitOk = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!useCacheConfig) {
                                    Toast.makeText(HomeFragment.this.requireContext(), getString(R.string.hm_ok), Toast.LENGTH_SHORT).show();
                                }
                                initData();
                            }
                        }, 50);
                    }

                    @Override
                    public void retry() {

                    }

                    @Override
                    public void error(String msg) {
                        jarInitOk = true;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if ("".equals(msg))
                                    Toast.makeText(HomeFragment.this.requireContext(), getString(R.string.hm_notok), Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(HomeFragment.this.requireContext(), msg, Toast.LENGTH_SHORT).show();
                                initData();
                            }
                        });
                    }
                });
            }
            return;
        }
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {
            TipDialog dialog = null;

            @Override
            public void retry() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                });
            }

            @Override
            public void success() {
                dataInitOk = true;
                if (ApiConfig.get().getSpider().isEmpty()) {
                    jarInitOk = true;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                }, 50);
            }

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dataInitOk = true;
                            jarInitOk = true;
                            initData();
                        }
                    });
                    return;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog == null)
                            dialog = new TipDialog(HomeFragment.this.requireContext(), msg, getString(R.string.hm_retry), getString(R.string.hm_cancel), new TipDialog.OnListener() {
                                @Override
                                public void left() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void right() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void cancel() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }
                            });
                        if (!dialog.isShowing())
                            dialog.show();
                    }
                });
            }
        }, this.requireActivity());
    }

    private void initViewPager(AbsSortXml absXml) {
        if (sortAdapter.getData().size() > 0) {
            for (MovieSort.SortData data : sortAdapter.getData()) {
                if (data.id.equals("my0")) {
                    if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList.size() > 0) {
                        fragments.add(UserFragment.newInstance(absXml.videoList));
                    } else {
                        fragments.add(UserFragment.newInstance(null));
                    }
                } else {
                    fragments.add(GridFragment.newInstance(data));
                }
            }
            pageAdapter = new HomePageAdapter(this.requireActivity().getSupportFragmentManager(), fragments);
            try {
                Field field = ViewPager.class.getDeclaredField("mScroller");
                field.setAccessible(true);
                FixedSpeedScroller scroller = new FixedSpeedScroller(this.requireContext(), new AccelerateInterpolator());
                field.set(mViewPager, scroller);
                scroller.setmDuration(300);
            } catch (Exception e) {
            }
            mViewPager.setPageTransformer(true, new DefaultTransformer());
            mViewPager.setAdapter(pageAdapter);
            mViewPager.setCurrentItem(currentSelected, false);
        }
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(getViewLifecycleOwner(), new Observer<AbsSortXml>() {
            @Override
            public void onChanged(AbsSortXml absXml) {
//                showSuccess();
                if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
                    sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), absXml.classes.sortList, true));
                } else {
                    sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), new ArrayList<>(), true));
                }
                initViewPager(absXml);
            }
        });
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        this.toolbar = binding.topAppBar;
        this.topLayout = binding.topLayout;
        this.contentLayout = binding.contentLayout;
        this.mGridView = binding.mGridViewCategory;
        this.mViewPager = binding.mViewPager;
        this.sortAdapter = new SortAdapter();
        this.mGridView.setLayoutManager(new V7LinearLayoutManager(this.requireContext(), 0, false));
        this.mGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.requireContext(), 10.0f));
        this.mGridView.setAdapter(this.sortAdapter);

        setLoadSir(this.contentLayout);

        return binding.getRoot();
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
