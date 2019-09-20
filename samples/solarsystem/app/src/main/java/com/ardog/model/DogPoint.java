package com.ardog.model;


import com.google.gson.Gson;

/**
 * {   "id":1,
 * "position":"x,y,z",
 * "filePath":"/sss/ss",
 * "rotation": "1,2,3,4",
 * "name":"会议室A",
 * "ids":"1,2,3",
 * }
 */
public class DogPoint {
    public long id;
    //位置点
    public float[] position;
    ///旋转点
    public float[] rotation;
    ////  模型的sdcard路径
    public String filePath;
    //名字
    public String name;
    ///临界点ids
    public String ids;

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    static public DogPoint parse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, DogPoint.class);
    }
}

