package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.ui.dialog.AlistDriveDialog;
import com.github.tvbox.osc.ui.dialog.WebdavDialog;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.StorageDriveType;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class DriveAdapter extends BaseQuickAdapter<DriveFolderFile, BaseViewHolder> {

    public DriveAdapter() {
        super(R.layout.item_drive, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, DriveFolderFile item) {
        TextView itemName = helper.getView(R.id.nameText);
        if(item.name == null && item.parentFolder == item)
            itemName.setText(" . . ");
        else
            itemName.setText(item.name);
        ImageView imgItem = helper.getView(R.id.imgItem);
        TextView lastModified = helper.getView(R.id.descriptionText);
        lastModified.setVisibility(View.GONE);
        if(item.isDrive())
        {
//            if(item.getDriveType() == StorageDriveType.TYPE.LOCAL)
//                imgItem.setImageResource(R.drawable.icon_sdcard);
//            else if(item.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
//                imgItem.setImageResource(R.drawable.icon_circle_node);
//            } else if(item.getDriveType() == StorageDriveType.TYPE.ALISTWEB) {
//                imgItem.setImageResource(R.drawable.icon_alist);
//            }
            imgItem.setImageResource(R.drawable.tab_file);
        } else {
            if(item.isFile) {
                lastModified.setText(item.getFormattedLastModified());
                lastModified.setVisibility(View.VISIBLE);
//            if(StorageDriveType.isVideoType(item.fileType)) {
//                imgItem.setImageResource(R.drawable.icon_film);
//            } else if (StorageDriveType.isImageType(item.fileType)) {
//                ImgUtil.load(item.fileUrl, imgItem,  0);
//            } else {
//                imgItem.setImageResource(R.drawable.icon_file);
//            }
                imgItem.setImageResource(R.drawable.driver_file);
            } else {
                imgItem.setImageResource(R.drawable.tab_file);
            }
        }
    }
}