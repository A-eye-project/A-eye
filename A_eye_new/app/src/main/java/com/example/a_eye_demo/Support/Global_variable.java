package com.example.a_eye_demo.Support;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class Global_variable extends Application {
    public static int choice;
    public static Bitmap img;
    public static String question;
    public static String imgString;
    public static String ttxString;
    public static String jsonString;
    public static JSONObject jsonObject;


    @Override
    public void onCreate(){
        img = null;
        question = "";
        ttxString = "";
        imgString = "";
        choice = 0;
        super.onCreate();
    }

    public static void set_imgString(){
        Bitmap resizedBmp = Bitmap.createScaledBitmap(img, 480, 620, true);
        imgString = BitmapToString(resizedBmp);
    }

    public static void set_Json() throws JSONException {
        jsonObject = new JSONObject();
        jsonObject.accumulate("Image", imgString);
        jsonObject.accumulate("Question", question);
        jsonString = jsonObject.toString();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String BitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        String temp = Base64.getEncoder().encodeToString(bytes);
        try {
            String res = new String(temp.getBytes(), "UTF-8");
            return res;
        } catch (Exception e){
            return "";
        }
    }
}
