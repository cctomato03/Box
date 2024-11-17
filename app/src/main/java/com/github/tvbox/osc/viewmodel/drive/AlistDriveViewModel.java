package com.github.tvbox.osc.viewmodel.drive;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.ui.activity.MainActivity;
import com.github.tvbox.osc.util.StorageDriveType;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.UA;
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.PostRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AlistDriveViewModel extends AbstractDriveViewModel {

    private void setRequestHeader(PostRequest request, String origin) {
        request.headers("User-Agent", UA.random());
        if (origin != null && !origin.isEmpty()) {
            if (origin.endsWith("/"))
                origin = origin.substring(0, origin.length() - 1);
            request.headers("origin", origin);
            request.headers("Referer", origin);
        }
        request.headers("accept", "application/json, text/plain, */*");
        request.headers("content-type", "application/json;charset=UTF-8");
    }

    public final String getUrl(String str) {
        String str2;
        if (str != null) {
            try {
                URL url = new URL(str);
                if (url.getPort() > 0) {
                    str2 = ":" + url.getPort();
                } else {
                    str2 = "";
                }
                return url.getProtocol() + "://" + url.getHost() + str2;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public String loadData(LoadDataCallback callback) {
        JsonObject config = currentDrive.getConfig();
        if (currentDrive.getChildren() == null) {
            new Thread() {
                public void run() {
                    String webLink = getUrl(config.get("url").getAsString());
                    try {
                        if (currentDrive.version == 0) {
                            String result = OkHttpUtil.get(webLink + "/api/public/settings");
                            JSONObject opt = new JSONObject(result);
                            Object obj = new JSONTokener(opt.optString("data")).nextValue();
                            if (obj instanceof JSONObject) {
                                currentDrive.version = 3;
                            } else if (obj instanceof JSONArray) {
                                currentDrive.version = 2;
                            }
                        }

                        if (currentDrive.version == 2) {
                            PostRequest<String> request = OkGo.<String>post(webLink + "/api/public/path").tag("drive");
                            JSONObject requestBody = new JSONObject();
                            requestBody.put("path", currentDrive.getPathStr());
                            requestBody.put("password", currentDrive.getConfig().get("password").getAsString());
                            requestBody.put("page_num", 1);
                            requestBody.put("page_size", 0);
                            request.upJson(requestBody);
                            setRequestHeader(request, webLink);
                            request.execute(new AbsCallback<String>() {

                                @Override
                                public String convertResponse(okhttp3.Response response) throws Throwable {
                                    return response.body().string();
                                }

                                @Override
                                public void onSuccess(Response<String> response) {
                                    String respBody = response.body();
                                    try {
                                        JsonObject respData = JsonParser.parseString(respBody).getAsJsonObject();
                                        List<DriveFolderFile> items = new ArrayList<>();
                                        if (respData.get("code").getAsInt() == 200) {
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                            for (JsonElement file : respData.get("data").getAsJsonObject().get("files").getAsJsonArray()) {
                                                JsonObject fileObj = file.getAsJsonObject();
                                                String fileName = fileObj.get("name").getAsString();
                                                int extNameStartIndex = fileName.lastIndexOf(".");
                                                boolean isFile = fileObj.get("type").getAsInt() != 1;
                                                String fileUrl = null;
                                                if (fileObj.has("url") && !fileObj.get("url").getAsString().isEmpty())
                                                    fileUrl = fileObj.get("url").getAsString();
                                                try {
                                                    DriveFolderFile driveFile = new DriveFolderFile(fileName, currentDrive.version, isFile,
                                                            isFile && extNameStartIndex >= 0 && extNameStartIndex < fileName.length() ?
                                                                    fileName.substring(extNameStartIndex + 1) : null,
                                                            dateFormat.parse(fileObj.get("updated_at").getAsString()).getTime());

                                                    driveFile.setConfig(config);
                                                    driveFile.setPathStr(currentDrive.getPathStr() + driveFile.name + "/");
                                                    driveFile.setDriveData(currentDrive.getDriveData());

                                                    if (fileUrl != null)
                                                        driveFile.fileUrl = fileUrl;

                                                    if (driveFile.isFile) {
                                                        if (StorageDriveType.isImageType(driveFile.fileType) || StorageDriveType.isVideoType(driveFile.fileType)) {
                                                            items.add(driveFile);
                                                        }
                                                    } else {
                                                        items.add(driveFile);
                                                    }
                                                } catch (ParseException e) {

                                                }
                                            }
                                        }
                                        sortData(items);
//                                        DriveFolderFile backItem = new DriveFolderFile(null, 0, false, null, null);
//                                        backItem.parentFolder = backItem;
//                                        items.add(0, backItem);
                                        currentDrive.setChildren(items);
                                        if (callback != null)
                                            callback.callback(currentDrive.getChildren(), false);
                                    } catch (Exception ex) {
                                        if (callback != null)
                                            callback.fail("无法访问，请注意地址格式");
                                    }
                                }
                            });
                        } else if (currentDrive.version == 3) {
                            PostRequest<String> request = OkGo.<String>post(webLink + "/api/fs/list").tag("drive");
                            JSONObject requestBody = new JSONObject();
                            requestBody.put("path", currentDrive.getPathStr());
                            requestBody.put("password", currentDrive.getConfig().get("password").getAsString());
                            requestBody.put("page", 1);
                            requestBody.put("per_page", 0);
                            requestBody.put("refresh", false);

                            request.upJson(requestBody);
                            setRequestHeader(request, webLink);
                            request.execute(new AbsCallback<String>() {

                                @Override
                                public String convertResponse(okhttp3.Response response) throws Throwable {
                                    return response.body().string();
                                }

                                @Override
                                public void onSuccess(Response<String> response) {
                                    String respBody = response.body();
                                    try {
                                        JsonObject respData = JsonParser.parseString(respBody).getAsJsonObject();
                                        List<DriveFolderFile> items = new ArrayList<>();
                                        if (respData.get("code").getAsInt() == 200) {
                                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                            for (JsonElement file : respData.get("data").getAsJsonObject().get("content").getAsJsonArray()) {
                                                JsonObject fileObj = file.getAsJsonObject();
                                                String fileName = fileObj.get("name").getAsString();
                                                int extNameStartIndex = fileName.lastIndexOf(".");
                                                boolean isFile = !fileObj.get("is_dir").getAsBoolean();

                                                try {
                                                    DriveFolderFile driveFile = new DriveFolderFile(fileName, currentDrive.version, isFile,
                                                            isFile && extNameStartIndex >= 0 && extNameStartIndex < fileName.length() ?
                                                                    fileName.substring(extNameStartIndex + 1) : null,
                                                            dateFormat.parse(fileObj.get("modified").getAsString()).getTime());
                                                    driveFile.setConfig(config);
                                                    driveFile.setPathStr(currentDrive.getPathStr() + driveFile.name + "/");
                                                    driveFile.setDriveData(currentDrive.getDriveData());

                                                    if (driveFile.isFile) {
                                                        if (StorageDriveType.isImageType(driveFile.fileType) || StorageDriveType.isVideoType(driveFile.fileType)) {
                                                            if (fileObj.get("sign") != null && !fileObj.get("sign").getAsString().isEmpty()) {
                                                                String fileUrl = webLink + "/p/" + driveFile.getPathStr();
                                                                fileUrl = StringUtils.subUrl(fileUrl);
                                                                String sign = fileObj.get("sign").getAsString();
                                                                fileUrl += "?sign=" + sign;
                                                                driveFile.fileUrl = fileUrl;
                                                            }
                                                            items.add(driveFile);
                                                        }
                                                    } else {
                                                        if (!driveFile.name.startsWith(".")) {
                                                            items.add(driveFile);
                                                        }
                                                    }
                                                } catch (ParseException ignored) {

                                                }
                                            }
                                        }
                                        sortData(items);
//                                        DriveFolderFile backItem = new DriveFolderFile(null, 0, false, null, null);
//                                        backItem.parentFolder = backItem;
//                                        items.add(0, backItem);
                                        currentDrive.setChildren(items);
                                        if (callback != null)
                                            callback.callback(currentDrive.getChildren(), false);
                                    } catch (Exception ex) {
                                        if (callback != null)
                                            callback.fail(MainActivity.getRes().getString(R.string.alist_connect_error));
                                    }
                                }
                            });
                        }
                    } catch (Exception ex) {
                        if (callback != null)
                            callback.fail(MainActivity.getRes().getString(R.string.alist_connect_error));
                    }
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

    public String getFileAddress(DriveFolderFile targetFile) {
        String webLink = getUrl(targetFile.getConfig().get("url").getAsString());
        try {
            PostRequest<String> request = OkGo.<String>post(webLink + "/api/fs/get");
            JSONObject requestBody = new JSONObject();
            requestBody.put("path", targetFile.getPathStr());
            requestBody.put("password", targetFile.getConfig().get("password").getAsString());
            request.upJson(requestBody);
            setRequestHeader(request, webLink);
            okhttp3.Response response = request.execute();
            JsonObject respData = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (respData.get("code").getAsInt() == 200) {
                return respData.get("data").getAsJsonObject().get("raw_url").getAsString();
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    public interface LoadFileCallback {
        void callback(String fileUrl);

        void fail(String msg);
    }
}