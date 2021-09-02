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
    // Selecting function variable
    public static int choice;

    // Camera Img variable
    public static Bitmap img;
    public static Bitmap resized;
    public static String question;
    public static String imgString;
    public static String ttxString;
    public static String jsonString;
    public static JSONObject jsonObject;

    // App Service variable
    public static boolean call_service = false;
    public static boolean behind_app = false;

    // Speech variable
    public static final float speech_threshold = 1e-5f;
    public static final String myKey = "adam";

    // Service Action
    public interface ACTION {
        String MAIN_ACTION = "com.example.a_eye.main";
        String START_FOREGROUND = "com.example.a_eye.start";
        String STOP_FOREGROUND = "com.example.a_eye.stop";
    }

    // Service Notification
    public interface NOTIFICATION {
        int FOREGROUND_SERVICE = 101;
        String CHANNEL_ID = "A_EYE";
    }

    @Override
    public void onCreate() {
        resized = null;
        img = null;
        question = "";
        ttxString = "";
        imgString = "";
        choice = 0;
        super.onCreate();
    }

    // Set Json
    public static void set_Json() throws JSONException {
        jsonObject = new JSONObject();
        jsonObject.accumulate("Image", imgString);
        jsonObject.accumulate("Question", question);
        jsonString = jsonObject.toString();
    }

    // Resized Img
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void set_imgString(boolean flag) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (flag) resized.compress(Bitmap.CompressFormat.PNG, 100, baos); // VQA
        else img.compress(Bitmap.CompressFormat.PNG, 100, baos);          // OCR

        byte[] bytes = baos.toByteArray();
        String temp = Base64.getEncoder().encodeToString(bytes);

        try {
            imgString = new String(temp.getBytes(), "UTF-8");
        } catch (Exception e) {
            imgString = "";
        }
    }
}
