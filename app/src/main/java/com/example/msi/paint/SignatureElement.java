package com.example.msi.paint;

import android.graphics.PointF;

import java.util.ArrayList;

public class SignatureElement{
    public ArrayList<SignaturePoint> mSignaturePoints;
    private long mTimeStart;

    public SignatureElement(long timeStart){
        mSignaturePoints = new ArrayList<>();
        mTimeStart = timeStart;
    }

    public void AddPoint(PointF pointToAdd, long timeOfAction){
        SignaturePoint sigPoint = new SignaturePoint(pointToAdd, timeOfAction - mTimeStart);
        mSignaturePoints.add(sigPoint);

    }

    public String toString(long signature_time){
        String point_info = "";
        for (SignaturePoint point : mSignaturePoints){
            point_info += mTimeStart - signature_time    + "," +
                          point.time    + "," +
                          point.point.x + "," +
                          point.point.y + "\n";
        }
        return point_info;
    }
}
