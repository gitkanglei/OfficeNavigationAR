package com.google.ar.sceneform.samples.solarsystem;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import cn.trinea.android.common.util.FileUtils;

/**
 * author: yangbang
 * date: 2019-09-20  18:45
 * fileName: PointUtil
 */
public class PointUtil {
    final static  String FILE_NAME="points.json";
   final static String FOLDER_NAME="/ArDog";
    static String list2String(List<DogPoint> points){
        Gson gson=new Gson();
      return  gson.toJson(points);
    }
    static List<DogPoint> json2List(String json){
        Gson gson=new Gson();
        Type collectionType = new TypeToken<List<DogPoint>>(){}.getType();
//        List<DogPoint> ints2 = gson.fromJson(json, collectionType);
       return gson.fromJson(json,collectionType);
    }
    ///保存数据到本地文件
    static void save2File(String json){
        String path=Environment.getExternalStorageDirectory().getAbsolutePath();
        path+=FOLDER_NAME;
        File folder=new File(path);
        if(!folder.exists()){
            folder.mkdirs();
        }
        path+="/"+FILE_NAME;
        File file=new File(path);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileUtils.writeFile(file.getAbsolutePath(),json);
    }
    //从文件中读取数据
    static String readFromFile(){
        String path=Environment.getExternalStorageDirectory().getAbsolutePath();
        path+=FOLDER_NAME;
        File folder=new File(path);
        if(!folder.exists()){
          return "";
        }
        path+="/"+FILE_NAME;
        File file=new File(path);
        if(!file.exists()){
            return "";
        }
       return FileUtils.readFile(path,"UTF-8").toString();

    }
}
