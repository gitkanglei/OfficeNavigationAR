package com.ardog.test;

import com.ardog.model.DogPoint;

import java.util.ArrayList;
import java.util.List;

public class TestDogPointData {

    public static List<DogPoint> generateFakePoint(){
        List<DogPoint> points = new ArrayList<>();

        DogPoint aPoint = new DogPoint(1,"A","2,3");
        DogPoint bPoint = new DogPoint(2,"B","3");
        DogPoint cPoint = new DogPoint(3,"C","5");
        DogPoint dPoint = new DogPoint(4,"D","1,5,6");
        DogPoint ePoint = new DogPoint(5,"E",null);
        DogPoint fPoint = new DogPoint(6,"F",null);

        points.add(aPoint);
        points.add(bPoint);
        points.add(cPoint);
        points.add(dPoint);
        points.add(ePoint);
        points.add(fPoint);

        return points;
    }
}
