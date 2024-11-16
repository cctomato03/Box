package com.github.tvbox.osc.bean;

import android.util.Base64;

import com.github.tvbox.osc.cache.StorageDrive;
import com.github.tvbox.osc.util.StorageDriveType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class DriveFolderFile {
    public String name;
    public int version = 0;
    public boolean isFile;
    public String fileType;
    private StorageDrive driveData;
    public Long lastModifiedDate;
    public String fileUrl;
    private String pathStr = "";
    private List<DriveFolderFile> children;
    private JsonObject config;

    public DriveFolderFile(StorageDrive driveData) {
        this.driveData = driveData;
        this.name = driveData.name;
        if(driveData.configJson != null && driveData.configJson.length() > 0)
            this.config = JsonParser.parseString(driveData.configJson).getAsJsonObject();
    }

    public DriveFolderFile(String name, int version, boolean isFile, String fileType, Long lastModifiedDate) {
        this.name = name;
        this.version = version;
        this.isFile = isFile;
        if(fileType != null)
            this.fileType = fileType.toUpperCase(Locale.ROOT);
        this.lastModifiedDate = lastModifiedDate;

    }

    public String getPathStr() {
        return this.pathStr;
    }

    public void setPathStr(String pathStr) {
        this.pathStr = pathStr;
    }

    public boolean isDrive() {
        return driveData != null && driveData.name != null;
    }

    public StorageDriveType.TYPE getDriveType() {
        return StorageDriveType.TYPE.values()[driveData.type];
    }

    public StorageDrive getDriveData() {
        return driveData;
    }

    public void setDriveData(StorageDrive driveData) {
        this.driveData = driveData;
    }

    public List<DriveFolderFile> getChildren() {
        return children;
    }

    public void setChildren(List<DriveFolderFile> children) {
        this.children = children;
    }

    public String getFormattedLastModified() {
        if(this.lastModifiedDate != null) {
            Date date = new Date(this.lastModifiedDate);
            Format fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");
            return fmt.format(date);
        }
        return "";
    }

    public JsonObject getConfig() {
        return config;
    }

    public void setConfig(JsonObject config) {
        this.config = config;
    }

    public String getWebDAVBase64Credential() {
        try {
            if (config.has("username") && config.has("password")) {
                byte[] data = (config.get("username").getAsString() + ":" + config.get("password").getAsString()).getBytes("UTF-8");
                return Base64.encodeToString(data, Base64.NO_WRAP);
            }
        }catch (Exception ex) {}
        return null;
    }
}