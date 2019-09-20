package com.ardog.utils;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

public class FileUtils {

    private FileUtils(){}

    public static boolean isFileExists(String path){
        if (TextUtils.isEmpty(path)){
            return false;
        }
        return new File(path).exists();
    }

    /**
     * 获取内置SD卡路径
     * @return
     */
    public static String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static String getARPath(){
        String storagePath = getInnerSDCardPath();
        storagePath += "/arcore";
        File f = new File(storagePath);
        if (!f.exists()){
            f.mkdirs();
        }
        return storagePath;
    }
}
