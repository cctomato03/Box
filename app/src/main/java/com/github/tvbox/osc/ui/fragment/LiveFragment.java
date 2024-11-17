package com.github.tvbox.osc.ui.fragment;

import static com.lzy.okgo.utils.HttpUtils.runOnUiThread;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.databinding.FragmentLiveBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.adapter.LivesAdapter;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.live.TxtSubscribe;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonArray;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import wseemann.media.FFmpegMediaMetadataRetriever;


public class LiveFragment extends Fragment {
    private FragmentLiveBinding binding;
    private final Handler mHandler = new Handler();
    private CircularProgressIndicator liveProgress;
    private final List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView mGridView;
    private LivesAdapter livesAdapter;

    private void initLiveState() {
        if (liveChannelGroupList.isEmpty()) {
            stopLoading(false);
        } else {
            stopLoading(true);
            tabLayout.removeAllTabs();
            for (int i = 0; i < liveChannelGroupList.size(); i++) {
                LiveChannelGroup group = liveChannelGroupList.get(i);
                tabLayout.addTab(tabLayout.newTab().setText(group.getGroupName()), i);
            }
            this.mGridView.requestLayout();
        }
    }

    //加载列表
    public void loadProxyLives(String url) {
        try {
            Uri parsedUrl = Uri.parse(url);
            url = new String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
            if (StringUtils.isEmpty(url)) {
                Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_live_url), Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Throwable th) {
            Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show();
            return;
        }
        OkGo.<String>get(url).execute(new AbsCallback<String>() {

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                return response.body().string();
            }

            @Override
            public void onSuccess(Response<String> response) {
                JsonArray livesArray;
                LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap = new LinkedHashMap<>();
                TxtSubscribe.parse(linkedHashMap, response.body());
                livesArray = TxtSubscribe.live2JsonArray(linkedHashMap);

                ApiConfig.get().loadLives(livesArray);
                List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
                if (list.isEmpty()) {
                    Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show();
                    return;
                }
                liveChannelGroupList.clear();
                liveChannelGroupList.addAll(list);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initLiveState();
                    }
                });
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                Toast.makeText(App.getInstance(), getString(R.string.act_live_play_network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initLiveChannelList() {
        List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
        if (list == null || list.isEmpty()) {
            Toast.makeText(App.getInstance(), getString(R.string.act_live_play_empty_channel), Toast.LENGTH_SHORT).show();
            return;
        }
        if (list.size() == 1 && list.get(0).getGroupName().startsWith("http://127.0.0.1")) {
            loadProxyLives(list.get(0).getGroupName());
        } else {
            liveChannelGroupList.clear();
            liveChannelGroupList.addAll(list);
            initLiveState();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        this.liveProgress = binding.liveProgress;
        this.toolbar = binding.livesTopBar;
        this.tabLayout = binding.tabLayout;
        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                List<LiveChannelItem> channelItemList = liveChannelGroupList.get(tab.getPosition()).getLiveChannels();
                livesAdapter.setNewData(channelItemList);
                if (channelItemList != null && !channelItemList.isEmpty()) {
                    new Thread() {
                        @Override
                        public void run() {
                            for (int index = 0; index < channelItemList.size(); index++) {
                                LiveChannelItem channelItem = channelItemList.get(index);
                                List<String> channelUrls = channelItem.getChannelUrls();

                                if (channelUrls != null && channelUrls.size() > channelItem.getSourceIndex()) {
                                    try {
                                        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
                                        String channelUrl = channelUrls.get(channelItem.getSourceIndex());
                                        mmr.setDataSource(channelUrl);
                                        Bitmap bitmap = mmr.getFrameAtTime();
                                        mmr.release();
                                        if (bitmap != null) {
                                            channelItem.channelPhoto = bitmap;
                                            livesAdapter.notifyItemChanged(index);
                                        }
                                    } catch (Exception ignored) {

                                    }
                                }
                            }
                        }
                    }.start();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        this.mGridView = binding.mGridView;
        this.livesAdapter = new LivesAdapter();
        mGridView.setAdapter(livesAdapter);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        this.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.lives_top_refresh) {
                    Intent intent = new Intent(requireContext(), LivePlayActivity.class);
                    startActivity(intent);
                }
                return false;
            }
        });
        this.stopLoading(false);
        EventBus.getDefault().register(this);
        return root;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_LIVEPLAY_UPDATE) {
            initLiveChannelList();
        }
    }

    private void startLoading() {
        this.tabLayout.setVisibility(View.GONE);
        this.mGridView.setVisibility(View.GONE);
        this.liveProgress.setVisibility(View.VISIBLE);
        this.liveProgress.setIndeterminate(true);
    }

    private void stopLoading(boolean success) {
        this.liveProgress.setIndeterminate(false);
        this.liveProgress.setVisibility(View.GONE);
        if (success) {
            this.tabLayout.setVisibility(View.VISIBLE);
            this.mGridView.setVisibility(View.VISIBLE);
        } else {
            this.tabLayout.setVisibility(View.GONE);
            this.mGridView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
