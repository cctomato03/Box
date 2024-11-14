package com.github.tvbox.osc.ui.fragment;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.StorageDrive;
import com.github.tvbox.osc.databinding.FragmentFilesBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DriveActivity;
import com.github.tvbox.osc.ui.activity.PlayActivity;
import com.github.tvbox.osc.ui.adapter.DriveAdapter;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import org.jetbrains.annotations.NotNull;

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
import me.jessyan.autosize.utils.AutoSizeUtils;

public class FilesFragment extends Fragment {
    private FragmentFilesBinding binding;
    private RecyclerView mGridView;
    private DriveAdapter adapter = new DriveAdapter();
    private MaterialToolbar toolbar;
    private List<DriveFolderFile> drives = null;
    List<DriveFolderFile> searchResult = null;
    private AbstractDriveViewModel backupViewModel = null;
    private int sortType = 0;
    private boolean isInSearch = false;
    private boolean delMode = false;
    private Handler mHandler = new Handler();
    private AbstractDriveViewModel viewModel = null;

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
//        jumpActivity(PlayActivity.class, bundle);
    }

    private void loadDriveData() {
        viewModel.setSortType(sortType);
        String path = viewModel.loadData(new AbstractDriveViewModel.LoadDataCallback() {
            @Override
            public void callback(List<DriveFolderFile> list, boolean alreadyHasChildren) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setNewData(viewModel.getCurrentDriveNote().getChildren());
                    }
                });
            }

            @Override
            public void fail(String message) {
                viewModel = null;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        if (StringUtils.isNotEmpty(path)) {
            this.toolbar.setTitle(path);
        }
    }

    private void openSortDialog() {
        List<String> options = Arrays.asList("按名字升序", "按名字降序", "按修改时间升序", "按修改时间降序");
        int sort = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);
        SelectDialog<String> dialog = new SelectDialog<>(requireContext());
        dialog.setTip("请选择列表排序方式");
        dialog.setAdapter(null, new SelectDialogAdapter.SelectDialogInterface<String>() {
            @Override
            public void click(String value, int pos) {
                sortType = pos;
                Hawk.put(HawkConfig.STORAGE_DRIVE_SORT, pos);
                dialog.dismiss();
                loadDriveData();
            }

            @Override
            public String getDisplay(String val) {
                return val;
            }
        }, null, options, sort);
        dialog.show();
    }

    public void toggleDelMode() {
        delMode = !delMode;
        adapter.toggleDelMode(delMode);
    }

    private void openFilePicker() {
        if (delMode)
            toggleDelMode();
        ChooserDialog dialog = new ChooserDialog(requireContext(), R.style.FileChooserStyle);
        dialog
                .withStringResources("选择一个文件夹", "确定", "取消")
                .titleFollowsDir(true)
                .displayPath(true)
                .enableDpad(true)
                .withFilter(true, true)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String dir, File dirFile) {
                        String absPath = dirFile.getAbsolutePath();
                        for (DriveFolderFile drive : drives) {
                            if (drive.getDriveType() == StorageDriveType.TYPE.LOCAL && absPath.equals(drive.getDriveData().name)) {
                                Toast.makeText(requireContext(), "此文件夹之前已被添加到空间列表！", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        RoomDataManger.insertDriveRecord(absPath, StorageDriveType.TYPE.LOCAL, null);
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_DRIVE_REFRESH));
                    }
                }).show();
    }

    private void openWebdavDialog(StorageDrive drive) {
        WebdavDialog webdavDialog = new WebdavDialog(requireContext(), drive);
        EventBus.getDefault().register(webdavDialog);
        webdavDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                EventBus.getDefault().unregister(dialog);
            }
        });
        webdavDialog.show();
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

    private void returnPreviousFolder() {
        if (isInSearch && viewModel == null) {
            //if already in search list
            isInSearch = false;
            viewModel = backupViewModel;
            backupViewModel = null;
            if (viewModel == null) {
                //if no last view list, return to main menu
                initData();
            } else {
                //return to last view list
                loadDriveData();
            }
            return;
        }
        viewModel.getCurrentDriveNote().setChildren(null);
        viewModel.setCurrentDriveNote(viewModel.getCurrentDriveNote().parentFolder);
        if (viewModel.getCurrentDriveNote() == null) {
            if (isInSearch) {
                //if returns from a search result, back to search result
                this.toolbar.setTitle("搜索结果");
                adapter.setNewData(searchResult);
                viewModel = null;
                return;
            }
            viewModel = null;
            initData();
            return;
        }
        loadDriveData();
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("drive");
    }

    private void initData() {
        this.toolbar.setTitle(getString(R.string.act_drive));
        sortType = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);
        if (drives == null) {
            drives = new ArrayList<>();
            List<StorageDrive> storageDrives = RoomDataManger.getAllDrives();
            for (StorageDrive storageDrive : storageDrives) {
                DriveFolderFile drive = new DriveFolderFile(storageDrive);
                if (delMode)
                    drive.isDelMode = true;
                drives.add(drive);
            }
        }
        adapter.setNewData(drives);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFilesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        this.mGridView = binding.mGridView;
        this.toolbar = binding.filesTopBar;

        this.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.files_top_add) {
                    FastClickCheckUtil.check(root);
                    StorageDriveType.TYPE[] types = StorageDriveType.TYPE.values();

                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
                    bottomSheetDialog.setContentView(R.layout.sheet_files_storage);
                    bottomSheetDialog.setDismissWithAnimation(true);

                    View webdavButton = bottomSheetDialog.findViewById(R.id.webdav_button);
                    webdavButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openWebdavDialog(null);
                            bottomSheetDialog.dismiss();
                        }
                    });

                    View alistButton = bottomSheetDialog.findViewById(R.id.alist_button);
                    alistButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            openAlistDriveDialog(null);
                            bottomSheetDialog.dismiss();
                        }
                    });

                    View localButton = bottomSheetDialog.findViewById(R.id.local_button);
                    localButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bottomSheetDialog.dismiss();
                            if (Build.VERSION.SDK_INT >= 23) {
                                if (App.getInstance().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(requireActivity(),
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    return;
                                }
                            }
                            openFilePicker();
                        }
                    });
                    bottomSheetDialog.show();
                } else if (item.getItemId() == R.id.files_top_delete) {
                    toggleDelMode();
                } else if (item.getItemId() == R.id.files_top_sort) {
                    FastClickCheckUtil.check(root);
                    openSortDialog();
                }
                return false;
            }
        });

        this.mGridView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        this.mGridView.setAdapter(this.adapter);
        this.adapter.bindToRecyclerView(this.mGridView);
        this.adapter.setOnItemClickListener((adapter, view, position) -> {
            if (delMode) {
                DriveFolderFile selectedDrive = drives.get(position);
                RoomDataManger.deleteDrive(selectedDrive.getDriveData().getId());
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_DRIVE_REFRESH));
                return;
            }
            DriveFolderFile selectedItem = ((DriveFolderFile) adapter.getItem(position));

            if ((selectedItem == selectedItem.parentFolder || selectedItem.parentFolder == null) && selectedItem.name == null) {
                returnPreviousFolder();
                return;
            }
            if (viewModel == null) {
                if (selectedItem.getDriveType() == StorageDriveType.TYPE.LOCAL) {
                    viewModel = new LocalDriveViewModel();
                } else if (selectedItem.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
                    viewModel = new WebDAVDriveViewModel();
                } else if (selectedItem.getDriveType() == StorageDriveType.TYPE.ALISTWEB) {
                    viewModel = new AlistDriveViewModel();
                }
                viewModel.setCurrentDrive(selectedItem);
                if (!selectedItem.isFile) {
                    loadDriveData();
                    return;
                }
            }

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
                                        Toast toast = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT);
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
                        DriveFolderFile driveFolderFile = ((DriveFolderFile) object);
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
                                .setContext(requireContext())
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
                                        File cacheFile = getGlideCacheFile(requireContext(), imageInfoList.get(position).getOriginUrl());
                                        if (cacheFile != null && cacheFile.exists()) {
                                            try {
                                                ExifInterface exifInterface = new ExifInterface(new FileInputStream(cacheFile));
                                                String downloadLink = exifInterface.getAttribute(ExifInterface.TAG_ARTIST);
                                                if (downloadLink != null && !downloadLink.isEmpty()) {
                                                    AlertDialog dialog = new AlertDialog.Builder(activity)
                                                            .setTitle("提示")
                                                            .setMessage("这里将会提取图片中的链接信息")
                                                            .setPositiveButton("复制", (dialog1, which) -> {
                                                                ClipboardManager clipboardManager =
                                                                        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                                                clipboardManager.setPrimaryClip(ClipData.newPlainText("label", downloadLink));
                                                            })
                                                            .setNegativeButton("打开", (dialog1, which) -> {
                                                                Intent openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadLink));
                                                                try {
                                                                    startActivity(openIntent);
                                                                } catch (
                                                                        ActivityNotFoundException e) {
                                                                    Toast.makeText(getContext(), "没有找到打开此链接的应用", Toast.LENGTH_SHORT).show();
                                                                }
                                                            })
                                                            .create();
                                                    dialog.show();
                                                }
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        } else {
                                            Toast.makeText(getContext(), "图片还未下载", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "Media Unsupported", Toast.LENGTH_SHORT).show();
                }
            }
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
