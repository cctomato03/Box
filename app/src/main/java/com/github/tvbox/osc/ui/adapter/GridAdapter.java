package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.ImgUtil;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class GridAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    public GridAdapter() {
        super(R.layout.item_grid , new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        helper.setText(R.id.tvName, item.name);

        ImageView ivThumb = helper.getView(R.id.ivThumb);
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            // takagen99 : Use Glide instead
            ImgUtil.load(item.pic, ivThumb,  0);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}