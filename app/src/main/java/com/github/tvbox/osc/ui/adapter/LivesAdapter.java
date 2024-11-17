package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ImgUtil;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.util.ArrayList;

public class LivesAdapter  extends BaseQuickAdapter<LiveChannelItem, BaseViewHolder> {
    public LivesAdapter() {
        super(R.layout.item_live , new ArrayList<>());
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, LiveChannelItem item) {
        helper.setText(R.id.tvName, item.getChannelName());

        ImageView ivThumb = helper.getView(R.id.ivThumb);

        //由于部分电视机使用glide报错
        if (item.channelPhoto != null) {
            ivThumb.setImageBitmap(item.channelPhoto);
        } else if (!StringUtils.isBlank(item.getChannelName())) {
            String[] epgInfo = EpgUtil.getEpgInfo(item.getChannelName());
            if (epgInfo != null) {
                ImgUtil.load(epgInfo[0], ivThumb, 0);
            } else {
                ivThumb.setImageResource(R.drawable.img_loading_placeholder);
            }
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}
