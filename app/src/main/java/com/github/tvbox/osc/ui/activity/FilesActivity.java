package com.github.tvbox.osc.ui.activity;

import static cc.shinichi.library.glide.ImageLoader.getGlideCacheFile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.GsonUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.StorageDrive;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.DriveAdapter;
import com.github.tvbox.osc.ui.adapter.FilesAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.AlistDriveDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.WebdavDialog;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import cc.shinichi.library.ImagePreview;
import cc.shinichi.library.bean.ImageInfo;
import cc.shinichi.library.view.listener.OnBigImageLongClickListener;
import cc.shinichi.library.view.listener.OnDownloadClickListener;

public class FilesActivity extends AppCompatActivity {

    private RecyclerView mGridView;
    private FilesAdapter adapter = new FilesAdapter();
    private CircularProgressIndicator filesProgress;
    private List<DriveFolderFile> drives = null;
    private AbstractDriveViewModel viewModel = null;
    private int sortType = 0;
    private MaterialToolbar toolbar;
    private Handler mHandler = new Handler();
    private DriveFolderFile item = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            item = GsonUtils.fromJson(intent.getStringExtra("viewModel"), DriveFolderFile.class);

            if (item.getDriveType() == StorageDriveType.TYPE.LOCAL) {
                viewModel = new LocalDriveViewModel();
            } else if (item.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
                viewModel = new WebDAVDriveViewModel();
            } else if (item.getDriveType() == StorageDriveType.TYPE.ALISTWEB) {
                viewModel = new AlistDriveViewModel();
            }
            viewModel.setCurrentDrive(item);
        }

        setContentView(R.layout.activity_files);

        initView();
        initData();
    }

    @Override
    public void onBackPressed() {
        returnPreviousFolder();
    }

    private void returnPreviousFolder() {
        if (viewModel.getCurrentDriveNote().parentFolder == null) {
            finish();
        } else {
            viewModel.getCurrentDriveNote().setChildren(null);
            viewModel.setCurrentDriveNote(viewModel.getCurrentDriveNote().parentFolder);
            if (viewModel.getCurrentDriveNote() == null) {
                viewModel = null;
                initData();
                return;
            }
            loadDriveData();
        }
    }

    private void startLoading() {
        this.filesProgress.setVisibility(View.VISIBLE);
        this.filesProgress.setIndeterminate(true);
    }

    private void stopLoading() {
        this.filesProgress.setVisibility(View.GONE);
        this.filesProgress.setIndeterminate(false);
    }

    private void initView() {
        this.mGridView = findViewById(R.id.mGridView);
        this.filesProgress = findViewById(R.id.files_progress);
        this.toolbar = findViewById(R.id.files_top_bar);

        this.toolbar.setNavigationIcon(R.drawable.navigation_back);

        this.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnPreviousFolder();
            }
        });

        this.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.files_top_sort) {
                    openSortDialog();
                } else if (item.getItemId() == R.id.files_top_refresh) {
                    loadDriveData();
                }
                return false;
            }
        });

        this.mGridView.setLayoutManager(new LinearLayoutManager(FilesActivity.this, RecyclerView.VERTICAL, false));
        this.mGridView.setAdapter(this.adapter);
        this.mGridView.setAdapter(this.adapter);
        this.adapter.bindToRecyclerView(this.mGridView);

        this.adapter.setOnItemClickListener((adapter, view, position) -> {
            DriveFolderFile selectedItem = ((DriveFolderFile) adapter.getItem(position));

            if (!selectedItem.isFile) {
                viewModel.setCurrentDriveNote(selectedItem);
                loadDriveData();
            } else {
                // takagen99 - To only play media file
                if (StorageDriveType.isVideoType(selectedItem.fileType)) {
                    DriveFolderFile currentDrive = viewModel.getCurrentDrive();
                    if (currentDrive.getDriveType() == StorageDriveType.TYPE.LOCAL)
                        playFile(currentDrive.name + selectedItem.getAccessingPathStr() + selectedItem.name);
                    else if (currentDrive.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
                        JsonObject config = currentDrive.getConfig();
                        String targetPath = selectedItem.getAccessingPathStr() + selectedItem.name;
                        playFile(config.get("url").getAsString() + targetPath);
                    } else if (currentDrive.getDriveType() == StorageDriveType.TYPE.ALISTWEB) {
                        AlistDriveViewModel boxedViewModel = (AlistDriveViewModel) viewModel;

                        boxedViewModel.loadFile(selectedItem, new AlistDriveViewModel.LoadFileCallback() {
                            @Override
                            public void callback(String fileUrl) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        playFile(fileUrl);
                                    }
                                });
                            }

                            @Override
                            public void fail(String msg) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast = Toast.makeText(FilesActivity.this, msg, Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                });
                            }
                        });
                    }
                } else if (StorageDriveType.isImageType(selectedItem.fileType) && viewModel.getCurrentDrive().getDriveType() == StorageDriveType.TYPE.WEBDAV) {
                    List<ImageInfo> imageInfoList = new ArrayList<>();
                    int imageIndex = 0;
                    for (Object object : adapter.getData()) {
                        DriveFolderFile driveFolderFile = (DriveFolderFile)object;

                        if (StorageDriveType.isImageType(driveFolderFile.fileType)) {
                            ImageInfo imageInfo = new ImageInfo();

                            JsonObject config = viewModel.getCurrentDrive().getConfig();
                            String targetPath = driveFolderFile.getAccessingPathStr() + driveFolderFile.name;
                            imageInfo.setOriginUrl(config.get("url").getAsString() + targetPath);
                            imageInfo.setName(driveFolderFile.name);

                            String credentialStr = viewModel.getCurrentDrive().getWebDAVBase64Credential();
                            if (credentialStr != null) {
                                HashMap<String, String> header = new HashMap<>();
                                header.put("authorization", "Basic " + credentialStr);
                                imageInfo.setHeader(header);
                            }

                            imageInfoList.add(imageInfo);
                            if (driveFolderFile == selectedItem) {
                                imageIndex = imageInfoList.size() - 1;
                            }
                        }
                    }
                    if (!imageInfoList.isEmpty()) {
                        ImagePreview.getInstance()
                                .setContext(FilesActivity.this)
                                .setIndex(imageIndex)
                                .setImageInfoList(imageInfoList)
                                .setLoadStrategy(ImagePreview.LoadStrategy.AlwaysOrigin)
                                .setLongPicDisplayMode(ImagePreview.LongPicDisplayMode.Default)
                                .setFolderName("webdavImage")
                                .setZoomTransitionDuration(300)
                                .setShowErrorToast(false)
                                .setEnableClickClose(false)
                                .setBigImageLongClickListener(new OnBigImageLongClickListener() {
                                    @Override
                                    public boolean onLongClick(Activity activity, View view, int position) {
                                        return true;
                                    }
                                })
                                .setDownloadClickListener(new OnDownloadClickListener() {
                                    @Override
                                    public void onClick(Activity activity, View view, int position) {
                                        // 可以在此处执行您自己的下载逻辑、埋点统计等信息
                                        File cacheFile = getGlideCacheFile(FilesActivity.this, imageInfoList.get(position).getOriginUrl());
                                        if (cacheFile != null && cacheFile.exists()) {
                                            try {
                                                ExifInterface exifInterface = new ExifInterface(new FileInputStream(cacheFile));
                                                String downloadLink = exifInterface.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST);
                                                if (downloadLink != null && !downloadLink.isEmpty()) {
                                                    AlertDialog dialog = new AlertDialog.Builder(activity)
                                                            .setTitle("提示")
                                                            .setMessage("这里将会提取图片中的链接信息")
                                                            .setPositiveButton("复制", (dialog1, which) -> {
                                                                ClipboardManager clipboardManager =
                                                                        (ClipboardManager) FilesActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                                                clipboardManager.setPrimaryClip(ClipData.newPlainText("label", downloadLink));
                                                            })
                                                            .setNegativeButton("打开", (dialog1, which) -> {
                                                                Intent openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
                                                                try {
                                                                    startActivity(openIntent);
                                                                } catch (
                                                                        ActivityNotFoundException e) {
                                                                    Toast.makeText(FilesActivity.this, "没有找到打开此链接的应用", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .create();
                                                    dialog.show();
                                                }
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        } else {
                                            Toast.makeText(FilesActivity.this, "图片还未下载", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public boolean isInterceptDownload() {
                                        // return true 时, 需要自己实现下载
                                        // return false 时, 使用内置下载
                                        return true;
                                    }
                                }).start();
                    }
                } else {
                    Toast.makeText(FilesActivity.this, "Media Unsupported", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void playFile(String fileUrl) {
        VodInfo vodInfo = new VodInfo();
        vodInfo.name = "存储";
        vodInfo.playFlag = "drive";
        DriveFolderFile currentDrive = viewModel.getCurrentDrive();
        if (currentDrive.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
            String credentialStr = currentDrive.getWebDAVBase64Credential();
            if (credentialStr != null) {
                JsonObject playerConfig = new JsonObject();
                JsonArray headers = new JsonArray();
                JsonElement authorization = JsonParser.parseString(
                        "{ \"name\": \"authorization\", \"value\": \"Basic " + credentialStr + "\" }");
                headers.add(authorization);
                playerConfig.add("headers", headers);
                vodInfo.playerCfg = playerConfig.toString();
            }
        }
        vodInfo.seriesFlags = new ArrayList<>();
        vodInfo.seriesFlags.add(new VodInfo.VodSeriesFlag("drive"));
        vodInfo.seriesMap = new LinkedHashMap<>();
        VodInfo.VodSeries series = new VodInfo.VodSeries(fileUrl, "tvbox-drive://" + fileUrl);
        List<VodInfo.VodSeries> seriesList = new ArrayList<>();
        seriesList.add(series);
        vodInfo.seriesMap.put("drive", seriesList);
        Bundle bundle = new Bundle();
        bundle.putBoolean("newSource", true);
        bundle.putString("sourceKey", "_drive");
        bundle.putSerializable("VodInfo", vodInfo);
        // takagen99 - to play file here zzzzzzzzzzzzzzz

        Intent intent = new Intent(FilesActivity.this, PlayActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void openSortDialog() {
        CharSequence[] choices = {MainActivity.getRes().getString(R.string.driver_sort_type_name_asc),
                MainActivity.getRes().getString(R.string.driver_sort_type_name_desc),
                MainActivity.getRes().getString(R.string.driver_sort_type_time_asc),
                MainActivity.getRes().getString(R.string.driver_sort_type_time_desc)
        };
        int sort = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);

        new MaterialAlertDialogBuilder(FilesActivity.this)
                .setTitle(MainActivity.getRes().getString(R.string.driver_sort_title))
                .setPositiveButton(
                        MainActivity.getRes().getString(R.string.confirm),
                        (DialogInterface dialog, int which) -> {
                            int checkedItemPosition =
                                    ((androidx.appcompat.app.AlertDialog) dialog).getListView().getCheckedItemPosition();
                            if (checkedItemPosition != AdapterView.INVALID_POSITION) {
                                sortType = checkedItemPosition;
                                Hawk.put(HawkConfig.STORAGE_DRIVE_SORT, checkedItemPosition);
                                loadDriveData();
                            }
                        })
                .setNegativeButton(MainActivity.getRes().getString(R.string.cancel), null)
                .setSingleChoiceItems(choices, sort, null)
                .show();
    }

    private void initData() {
        this.drives = null;
        sortType = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);
        loadDriveData();
    }

    private void loadDriveData() {
        startLoading();
        viewModel.setSortType(sortType);
        String path = viewModel.loadData(new AbstractDriveViewModel.LoadDataCallback() {
            @Override
            public void callback(List<DriveFolderFile> list, boolean alreadyHasChildren) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopLoading();
                        adapter.setNewData(viewModel.getCurrentDriveNote().getChildren());
                    }
                });
            }

            @Override
            public void fail(String message) {

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopLoading();
                        Toast.makeText(FilesActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        if (StringUtils.isNotEmpty(path)) {
            this.toolbar.setTitle(path);
        } else {
            this.toolbar.setTitle(item.name);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
