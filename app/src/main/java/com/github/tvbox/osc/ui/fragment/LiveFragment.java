package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.databinding.FragmentLiveBinding;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.util.JavaUtil;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.live.TxtSubscribe;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.JsonArray;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import kotlin.Pair;

public class LiveFragment extends Fragment {
    private FragmentLiveBinding binding;
    private final Handler mHandler = new Handler();
    private CircularProgressIndicator liveProgress;
    private final List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private MaterialToolbar toolbar;

    private void initLiveState() {
        int lastChannelGroupIndex = -1;
        int lastLiveChannelIndex = -1;

        Pair<Integer, Integer> lastChannel = JavaUtil.findLiveLastChannel(liveChannelGroupList);
        lastChannelGroupIndex = lastChannel.getFirst();
        lastLiveChannelIndex = lastChannel.getSecond();

//        liveChannelGroupAdapter.setNewData(liveChannelGroupList);
//        selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex);
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
        startLoading();
        OkGo.<String>get(url).execute(new AbsCallback<String>() {

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                return response.body().string();
            }

            @Override
            public void onSuccess(Response<String> response) {
                stopLoading();
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
                stopLoading();
                Toast.makeText(App.getInstance(), getString(R.string.act_live_play_network_error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startLoading() {
        this.liveProgress.setVisibility(View.VISIBLE);
        this.liveProgress.setIndeterminate(true);
    }

    private void stopLoading() {
        this.liveProgress.setVisibility(View.GONE);
        this.liveProgress.setIndeterminate(false);
    }

    private void initLiveChannelList() {
        List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
        if (list.isEmpty()) {
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        this.liveProgress = binding.liveProgress;
        this.toolbar = binding.livesTopBar;
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
        this.stopLoading();

//        initLiveChannelList();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
