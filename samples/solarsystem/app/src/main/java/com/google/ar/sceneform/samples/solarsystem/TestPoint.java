package com.google.ar.sceneform.samples.solarsystem;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.ardog.model.DogPoint;
import com.ardog.utils.PointUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.schedulers.Schedulers;

/**
 * author: yangbang
 * date: 2019-09-20  18:51
 * fileName: MainActivity
 */
public class TestPoint extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        findViewById(R.id.btn_click).setOnClickListener((v)->{
            DogPoint point=new DogPoint();
            point.id=1;
            point.filePath="/dsds/fsfsf";
            point.ids="1,2,3";
            point.name="会议室1";
            point.position="0,0,2";
            point.rotation="1,0,3";
            Log.i("MainActivity","point: "+point.toJsonString());
            DogPoint point2=new DogPoint();
            point2.id=1;
            point2.filePath="/dsds/fsfsf";
            point2.ids="1,2,3";
            point2.name="会议室1";
            point2.position="0,0,2";
            point2.rotation="1,0,3";
//            Log.i("MainActivity","point: "+point2.toJsonString());
            List<DogPoint> points =new ArrayList<>();
            points.add(point);
            points.add(point);
          final String json=  PointUtil.list2String(points);
//            Log.i("MainActivity","point: "+json);
//            List<DogPoint> pointList=PointUtil.json2List(json);
//            Log.i("MainActivity","size: "+pointList.size());
            RxPermissions permissions=new RxPermissions(this);
            permissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    .observeOn(Schedulers.io())
                    .subscribe(isGranted->{
                        if(isGranted){
                            PointUtil.save2File(json);
                            String jsonStr=  PointUtil.readFromFile();
                            Log.i("MainActivity","jsonStr: "+jsonStr);
                        }
                    });

//            new Thread(()->{
//                PointUtil.save2File(json);
//                String jsonStr=  PointUtil.readFromFile();
//                Log.i("MainActivity","jsonStr: "+jsonStr);
//            }).start();


//            point.toJsonString();
        });
    }
}
