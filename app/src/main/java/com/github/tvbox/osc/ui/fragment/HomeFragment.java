package com.github.tvbox.osc.ui.fragment;

import static android.content.Intent.getIntent;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
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
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.databinding.FragmentHomeBinding;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ViewUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeFragment extends Fragment {
    private static Resources res;

    private List<TabLayout> tabLayouts;
    private ViewPager2 viewPager;
    private TabLayout listItem;
    private MaterialToolbar toolbar;
    private SourceViewModel sourceViewModel;
    private SortAdapter sortAdapter;
    private HomePageAdapter pageAdapter;
    private FragmentHomeBinding binding;
    boolean useCacheConfig = false;
    private final List<BaseLazyFragment> fragments = new ArrayList<>();
    private int currentSelected = 0;
    private final Handler mHandler = new Handler();

    public static String getResString(int resId) {
        return res.getString(resId);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        res = getResources();

        useCacheConfig = false;

        initViewModel();
        initData();

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
        }

        if (dataInitOk && jarInitOk) {
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            return;
        }
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

                    }
                });
            }
        }, this.requireActivity());
    }

    private void initViewPager(AbsSortXml absXml) {
        if (sortAdapter.getData().size() > 0) {
            try {
                ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager(), getLifecycle());
                for (MovieSort.SortData data : sortAdapter.getData()) {
                    if (data.id.equals("my0")) {
                        if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && absXml.videoList.size() > 0) {
                            adapter.addFragment(UserFragment.newInstance(absXml.videoList));
                        } else {
                            adapter.addFragment(UserFragment.newInstance(null));
                        }
                    } else {
                        adapter.addFragment(GridFragment.newInstance(data));
                    }
                }
//                adapter.addFragment(new LiveFragment());
                viewPager.setAdapter(adapter);
                for (TabLayout tabLayout : tabLayouts) {
                    new TabLayoutMediator(
                            tabLayout, viewPager, (tab, position) -> tab.setText(sortAdapter.getData().get(position).name))
                            .attach();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(getViewLifecycleOwner(), new Observer<AbsSortXml>() {
            @Override
            public void onChanged(AbsSortXml absXml) {
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
        tabLayouts = ViewUtil.findViewsWithType(binding.getRoot(), TabLayout.class);
        this.toolbar = binding.topAppBar;
        this.listItem = binding.listItem;
        this.viewPager = binding.viewpager;
        this.sortAdapter = new SortAdapter();

        return binding.getRoot();
    }

    static class ViewPagerAdapter extends FragmentStateAdapter {
        private final ArrayList<Fragment> arrayList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fragmentManager, Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        public void addFragment(Fragment fragment) {
            arrayList.add(fragment);
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        @Override
        public Fragment createFragment(int position) {
            return arrayList.get(position);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
