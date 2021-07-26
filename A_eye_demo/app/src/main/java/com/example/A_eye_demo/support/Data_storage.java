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

    @Override
    public void onCreate(){
        Flag = false;
        img = null;
        img_path = "";
        question = "";
        ttxString = "";
        choice = 0;
        super.onCreate();
    }
    public Bitmap getImg(){
        return img;
    }
}