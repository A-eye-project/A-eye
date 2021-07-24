package com.example.A_eye_demo.support;

import android.app.Application;

import android.graphics.Bitmap;

public class Data_storage extends Application {
    public static Bitmap img;
    public static Boolean Flag;
    public static String img_path;

    @Override
    public void onCreate(){
        Flag = false;
        img = null;
        img_path = "";
        super.onCreate();
    }
    public Bitmap getImg(){
        return img;
    }
}