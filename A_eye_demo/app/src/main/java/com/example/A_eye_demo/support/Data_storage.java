package com.example.A_eye_demo.support;

import android.app.Application;

import android.graphics.Bitmap;

public class Data_storage extends Application {
    public static Bitmap img;
    public static Boolean Flag;
    public static String img_path;
    public static String question;
    public static String ttxString;
    public static int choice;
    public static Boolean Upload_Status;

    @Override
    public void onCreate() {
        Flag = false;
        img = null;
        img_path = "";
        question = "";
        ttxString = "";
        choice = 0;
        Upload_Status = false;
        super.onCreate();
    }

    public Bitmap getImg(){
        return img;
    }
}