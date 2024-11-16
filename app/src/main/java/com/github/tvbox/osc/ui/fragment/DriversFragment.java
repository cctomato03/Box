package com.github.tvbox.osc.ui.fragment;

import static cc.shinichi.library.glide.ImageLoader.getGlideCacheFile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.GsonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.StorageDrive;
import com.github.tvbox.osc.databinding.FragmentDriversBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.activity.FilesActivity;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.ui.adapter.DriveAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.AlistDriveDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.StorageDriveType;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.viewmodel.drive.AbstractDriveViewModel;
import com.github.tvbox.osc.viewmodel.drive.AlistDriveViewModel;
import com.github.tvbox.osc.viewmodel.drive.LocalDriveViewModel;
import com.github.tvbox.osc.viewmodel.drive.WebDAVDriveViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnDownloadClickListener;

public class DriversFragment extends Fragment {
    private FragmentDriversBinding binding;
    private RecyclerView mGridView;
    private DriveAdapter adapter = new DriveAdapter();
    private MaterialToolbar toolbar;
    private List<DriveFolderFile> drives = null;
    private int sortType = 0;
    private boolean isInSearch = false;
    private Handler mHandler = new Handler();
    private AbstractDriveViewModel viewModel = null;

    private void openWebdavDialog(StorageDrive drive) {
        MaterialAlertDialogBuilder webdavBuilder = new MaterialAlertDialogBuilder(requireContext());
        View contentView = getLayoutInflater().inflate(R.layout.webdav_add, null);
        webdavBuilder.setTitle(R.string.bottom_webdav)
                .setView(contentView)
                .setCancelable(false);
        AlertDialog alertDialog = webdavBuilder.show();

        TextInputEditText namespaceText = contentView.findViewById(R.id.webdav_content_namespace);
        TextInputEditText addressText = contentView.findViewById(R.id.webdav_content_address);
        TextInputEditText pathText = contentView.findViewById(R.id.webdav_content_path);
        TextInputEditText usernameText = contentView.findViewById(R.id.webdav_content_username);
        TextInputEditText passwordText = contentView.findViewById(R.id.webdav_content_password);
        if (drive != null) {
            namespaceText.setText(drive.name);
            try {
                JsonObject config = JsonParser.parseString(drive.configJson).getAsJsonObject();
                addressText.setText(config.get("url").getAsString());
                pathText.setText(config.get("initPath").getAsString());
                usernameText.setText(config.get("username").getAsString());
                passwordText.setText(config.get("password").getAsString());
            } catch (Exception ignored) {

            }
        }

        Button confirmButton = contentView.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(view -> {
            String name = Objects.requireNonNull(namespaceText.getText()).toString();
            String url = Objects.requireNonNull(addressText.getText()).toString();
            String initPath = Objects.requireNonNull(pathText.getText()).toString();
            String username = Objects.requireNonNull(usernameText.getText()).toString();
            String password = Objects.requireNonNull(passwordText.getText()).toString();
            if(name.isEmpty())
            {
                Toast.makeText(requireContext(), "请赋予一个空间名称", Toast.LENGTH_SHORT).show();
                return;
            }
            if(url.isEmpty())
            {
                Toast.makeText(requireContext(), "请务必填入WebDav地址", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!url.endsWith("/"))
                url += "/";
            JsonObject config = new JsonObject();
            config.addProperty("url", url);
            if(initPath.startsWith("/"))
                initPath = initPath.substring(1);
            if(initPath.endsWith("/"))
                initPath = initPath.substring(0, initPath.length() - 1);
            config.addProperty("initPath", initPath);
            config.addProperty("username", username);
            config.addProperty("password", password);
            if(drive != null) {
                drive.name = name;
                drive.configJson = config.toString();
                RoomDataManger.updateDriveRecord(drive);
            } else {
                RoomDataManger.insertDriveRecord(name, StorageDriveType.TYPE.WEBDAV, config);
            }
            this.initData();
            alertDialog.dismiss();
        });
        Button cancelButton = contentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(view -> {
            alertDialog.dismiss();
        });
    }

    private void openAlistDriveDialog(StorageDrive drive) {
        AlistDriveDialog dialog = new AlistDriveDialog(requireContext(), drive);
        EventBus.getDefault().register(dialog);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                EventBus.getDefault().unregister(dialog);
            }
        });
        dialog.show();
    }

    private void initData() {
        this.drives = null;
        this.toolbar.setTitle(getString(R.string.act_drive));
        sortType = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);

        drives = new ArrayList<>();
        List<StorageDrive> storageDrives = RoomDataManger.getAllDrives();

        StorageDrive localStorage = new StorageDrive();
//        localStorage.name = MainActivity.getRes().getString(R.string.bottom_local);
        localStorage.name = "/storage/emulated/0/";
        localStorage.type = StorageDriveType.TYPE.LOCAL.ordinal();
        localStorage.configJson = null;

        DriveFolderFile localDriver = new DriveFolderFile(localStorage);
        localDriver.setConfig(new JsonObject());
        drives.add(localDriver);

        for (StorageDrive storageDrive : storageDrives) {
            DriveFolderFile drive = new DriveFolderFile(storageDrive);
            drives.add(drive);
        }
        adapter.setNewData(drives);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDriversBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        this.mGridView = binding.mGridView;
        this.toolbar = binding.driversTopBar;

        this.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.files_top_add) {
                    FastClickCheckUtil.check(root);

                    CharSequence[] choices = {MainActivity.getRes().getString(R.string.bottom_webdav), MainActivity.getRes().getString(R.string.bottom_alist)};
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(MainActivity.getRes().getString(R.string.bottom_storage_type))
                            .setItems(choices, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 0) {
                                        openWebdavDialog(null);
                                    } else if (i == 1) {
                                        openAlistDriveDialog(null);
                                    }
                                }
                            }).show();
                }
                return false;
            }
        });

        this.mGridView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false));
        this.mGridView.setAdapter(this.adapter);
        this.adapter.bindToRecyclerView(this.mGridView);

        this.adapter.setOnItemLongClickListener((adapter, view, position) -> {
            CharSequence[] choices = {MainActivity.getRes().getString(R.string.driver_edit_operation), MainActivity.getRes().getString(R.string.driver_delete_operation)};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(MainActivity.getRes().getString(R.string.driver_choose_operation))
                    .setItems(choices, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (i == 0) {
                                openWebdavDialog(((DriveFolderFile) adapter.getItem(position)).getDriveData());
                            } else if (i == 1) {
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.driver_delete)
                                        .setNegativeButton(R.string.cancel, null)
                                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                DriveFolderFile selectedDrive = drives.get(position);
                                                RoomDataManger.deleteDrive(selectedDrive.getDriveData().getId());
                                                initData();
                                            }
                                        }).show();
                            }
                        }
                    }).show();
            return false;
        });

        this.adapter.setOnItemClickListener((adapter, view, position) -> {
            DriveFolderFile selectedItem = ((DriveFolderFile) adapter.getItem(position));

            Bundle bundle = new Bundle();
            bundle.putString("viewModel", GsonUtils.toJson(selectedItem));

            Intent intent = new Intent(requireContext(), FilesActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        initData();
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
