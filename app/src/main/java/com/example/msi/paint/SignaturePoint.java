package com.example.msi.paint;

import android.graphics.PointF;

public class SignaturePoint{
    public PointF point;
    public long time;

    public SignaturePoint(PointF pt, long tm){
        point = pt;
        time = tm;
    }
}
