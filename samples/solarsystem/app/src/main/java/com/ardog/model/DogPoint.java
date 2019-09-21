package com.ardog.model;


import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
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
    public Vector3 toPositionVector(){
        if(position==null || position.length!=3)return Vector3.zero();
        return new Vector3(position[0],position[1],position[2]);
    }
    public Vector3 toRotationVector(){
        if(rotation==null || rotation.length!=4)return Vector3.zero();
        return new Vector3(rotation[0],rotation[1],rotation[2]);
    }
    public Quaternion toQuaternion(){
        if(rotation==null || rotation.length!=4)return new Quaternion(0f,0f,0f,1f);
        return new Quaternion(rotation[0],rotation[1],rotation[2],rotation[3]);
    }
}

