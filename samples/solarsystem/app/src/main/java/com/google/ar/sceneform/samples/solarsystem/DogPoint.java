package com.google.ar.sceneform.samples.solarsystem;


import android.util.JsonWriter;

import com.google.gson.Gson;

import cn.trinea.android.common.util.FileUtils;

/**
{   "id":1,
    "position":"x,y,z",
     "filePath":"/sss/ss",
     "rotation": "1,2,3,4",
    "name":"会议室A",
    "ids":"1,2,3",
        }
 */
public class DogPoint {
    public long id;
    //位置点
    public String position;
    ///旋转点
    public String rotation;
    ////  模型的sdcard路径
    public  String filePath;
    //名字
    public String name;
    ///临界点ids
    public String ids;
    String toJsonString(){
        Gson gson=new Gson();
      return  gson.toJson(this);
    }
    DogPoint parse(String json){
        Gson gson=new Gson();
      return  gson.fromJson(json,DogPoint.class);
    }
}

