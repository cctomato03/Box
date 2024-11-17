package com.github.tvbox.osc.viewmodel.drive;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.util.StorageDriveType;
import com.google.gson.JsonObject;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WebDAVDriveViewModel extends AbstractDriveViewModel {

    private Sardine webDAV;

    private boolean initWebDav() {
        if (webDAV != null)
            return true;
        try {
            JsonObject config = currentDrive.getConfig();
            webDAV = new OkHttpSardine();
            if (config.has("username") && config.has("password")) {
                webDAV.setCredentials(config.get("username").getAsString(), config.get("password").getAsString());
            }
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private Sardine getWebDAV() {
        if (initWebDav()) {
            return webDAV;
        }
        return null;
    }

    @Override
    public String loadData(LoadDataCallback callback) {
        JsonObject config = currentDrive.getConfig();
        if (currentDrive.getChildren() == null) {
            new Thread() {
                public void run() {
                    Sardine webDAV = getWebDAV();
                    if (webDAV == null && callback != null) {
                        callback.fail(MainActivity.getRes().getString(R.string.webdav_connect_error));
                        return;
                    }
                    List<DavResource> files = null;
                    try {
                        String url = config.get("url").getAsString()  + currentDrive.getPathStr();
                        files = webDAV.list(url);
                    } catch (Exception ex) {
                        if (callback != null)
                            callback.fail(MainActivity.getRes().getString(R.string.webdav_connect_error));
                        return;
                    }

                    List<DriveFolderFile> items = new ArrayList<>();
                    if (files != null && files.size() > 1) {
                        for (int index = 1; index < files.size(); index++) {
                            DavResource file = files.get(index);
                            int extNameStartIndex = file.getName().lastIndexOf(".");
                            DriveFolderFile driveFolderFile = new DriveFolderFile(file.getName(), 0, !file.isDirectory(),
                                    !file.isDirectory() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                            file.getName().substring(extNameStartIndex + 1) : null,
                                    file.getModified().getTime());
                            driveFolderFile.setConfig(config);
                            driveFolderFile.setPathStr(currentDrive.getPathStr() + driveFolderFile.name + "/");
                            driveFolderFile.setDriveData(currentDrive.getDriveData());

                            if (driveFolderFile.isFile) {
                                if (StorageDriveType.isImageType(driveFolderFile.fileType) || StorageDriveType.isVideoType(driveFolderFile.fileType)) {
                                    items.add(driveFolderFile);
                                }
                            } else {
                                if (!driveFolderFile.name.startsWith(".")) {
                                    items.add(driveFolderFile);
                                }
                            }
                        }
                    }
                    sortData(items);
//                    DriveFolderFile backItem = new DriveFolderFile(null, null, 0, false, null, null);
//                    backItem.parentFolder = backItem;
//                    items.add(0, backItem);
                    currentDrive.setChildren(items);
                    if (callback != null)
                        callback.callback(currentDrive.getChildren(), false);

                }
            }.start();
            return currentDrive.name;
        } else {
            sortData(currentDrive.getChildren());
            if (callback != null)
                callback.callback(currentDrive.getChildren(), true);
        }
        return currentDrive.name;
    }

}