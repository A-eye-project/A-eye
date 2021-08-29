package com.example.a_eye.Support;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Build;

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

    public interface ACTION {
        String MAIN_ACTION = "com.example.a_eye.main";
        String START_FOREGROUND = "com.example.a_eye.start";
        String STOP_FOREGROUND = "com.example.a_eye.stop";
    }

    public interface NOTIFICATION {
        int FOREGROUND_SERVICE = 101;
        String CHANNEL_ID = "A_EYE";
    }

    @Override
    public void onCreate(){
        img = null;
        question = "";
        ttxString = "";
        imgString = "";
        choice = 0;
        super.onCreate();
    }

    public static void set_Json() throws JSONException {
        jsonObject = new JSONObject();
        jsonObject.accumulate("Image", imgString);
        jsonObject.accumulate("Question", question);
        jsonString = jsonObject.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void set_imgString(boolean flag) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(flag == true) Bitmap.createScaledBitmap(img, 480, 620, true).compress(Bitmap.CompressFormat.PNG, 100, baos);
        else img.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        String temp = Base64.getEncoder().encodeToString(bytes);
        try {
            imgString = new String(temp.getBytes(), "UTF-8");
        } catch (Exception e){
            imgString = "";
        }
    }
}
