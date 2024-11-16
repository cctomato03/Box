package com.github.tvbox.osc.viewmodel.drive;

import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.util.StorageDriveType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class LocalDriveViewModel extends AbstractDriveViewModel {

    @Override
    public String loadData(LoadDataCallback callback) {
        if (currentDrive.getChildren() == null) {
            File[] files = (new File(currentDrive.getPathStr())).listFiles();
            List<DriveFolderFile> items = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    int extNameStartIndex = file.getName().lastIndexOf(".");

                    DriveFolderFile driveFolderFile = new DriveFolderFile(file.getName(), 0, file.isFile(),
                            file.isFile() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                    file.getName().substring(extNameStartIndex + 1) : null,
                            file.lastModified());
                    driveFolderFile.setConfig(currentDrive.getConfig());
                    driveFolderFile.setPathStr(currentDrive.getPathStr() + driveFolderFile.name + "/");
                    driveFolderFile.setDriveData(currentDrive.getDriveData());

                    if (driveFolderFile.isFile) {
                        if (StorageDriveType.isImageType(driveFolderFile.fileType) || StorageDriveType.isVideoType(driveFolderFile.fileType)) {
                            items.add(driveFolderFile);
                        }
                    } else {
                        items.add(driveFolderFile);
                    }
                }
            }
            sortData(items);
//            DriveFolderFile backItem = new DriveFolderFile(null, null, 0, false, null, null);
//            backItem.parentFolder = backItem;
//            items.add(0, backItem);
            currentDrive.setChildren(items);
            if (callback != null) {
                callback.callback(currentDrive.getChildren(), false);
            }
        } else {
            sortData(currentDrive.getChildren());
            callback.callback(currentDrive.getChildren(), true);
        }
        return currentDrive.name;
    }

}