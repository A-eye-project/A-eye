package com.example.A_eye_demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.ImageCaptioning;
import com.example.A_eye_demo.support.OCR;
import com.example.A_eye_demo.support.TTSAdapter;
import com.example.A_eye_demo.support.VQA;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/*
 main : pocket_set(), resume(), ImageUploadToServer(), BitmapToString()
 pocketsphinx, OCR, ImageCaptioning, VQA, TTSAdpter

 */

public class TotalClass {
    private TextView myTv;
    private Context myContext;

    private pocketsphinx pocketTEST;
    ProgressDialog progressDialog;

    ImageCaptioning imgCaptioning = new ImageCaptioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();
    TTSAdapter tts = null;

    public void setup(Context mainContext, TextView tv) {
        myContext = mainContext;
        myTv = tv;

        tts = TTSAdapter.getInstance(myContext);
        pocketTEST = new pocketsphinx(myContext, tv);
    }

    public void pocket_set() {
        Handler n = new Handler();
        n.postDelayed(new Runnable() {
            @Override
            public void run() {
                if ((pocketTEST.Flag == true) && (pocketTEST.isalive == false)) {
                    pocketTEST.onSetup();
                    myTv.setText("Tell '" + pocketTEST.get_keyword() + "'");
                } else {
                    Log.i("Target", "What??");
                }
            }
        }, 2000);
    }

    public void resume(Context mainContext) {
        Handler n = new Handler();
        n.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Data_storage.Upload_Status == false) ImageUploadToServer(mainContext);
                if (Data_storage.ttxString != null) {
                    Intent intent = new Intent(mainContext.getApplicationContext(), Image_view.class);
                    mainContext.startActivity(intent);
                    Data_storage.Upload_Status = false;
                    myTv.setText("");
                } else {
                    Log.i("E", "ServerError");
                    resume(mainContext);
                }
            }
        }, 1000);
    }

    public String get_pocket_keyword() { return pocketTEST.get_keyword(); }
    public boolean get_pocket_isalive() { return pocketTEST.isalive; }
    public boolean get_pocket_flag() { return pocketTEST.Flag; }
    public void set_pocket_flag(Boolean f) { pocketTEST.Flag = f; }
    public void cancel() { pocketTEST.cancel(); }

    public void ImageUploadToServer(Context mainContext) {
        Data_storage.Upload_Status = true;
        class AsyncTaskUploadClass extends AsyncTask<Void, Void, String> {

            @Override
            protected String doInBackground(Void... params) { // BackGround에서 동작하는 부분.

                String res = null;
                String uploadImg = null;

                switch (Data_storage.choice) {
                    case 0: // OCR
                        uploadImg = BitmapToString(Data_storage.img);
                        res = ocr.getOcrRes(uploadImg);
                        break;

                    case 1: // Image Caption
                        res = imgCaptioning.getCaption(Data_storage.img);
                        break;

                    case 2: // VQA
                        uploadImg = BitmapToString(Data_storage.img);
                        res = vqa.getAns(uploadImg, Data_storage.question);
                        break;

                    default: // 이쪽으로 온다면 에러.
                        Log.i("Image Upload","invalid choice");
                        break;
                }

                if (res != "") {
                    Data_storage.ttxString = res;
                }

                Log.i("target",res);
                return res;
            }

            @Override
            protected void onPreExecute() { // BackGround 작업이 시작되기 전에 맨처음에 작동하는 부분.

                super.onPreExecute();

                // Showing progress dialog at image upload time.
                progressDialog = ProgressDialog.show(mainContext, "Image is Uploading", "Please Wait", false, false);
            }

            @Override
            protected void onPostExecute(String string1) {// 맨 마지막에 한번만 실행되는 부분.

                super.onPostExecute(string1);

                progressDialog.dismiss();

                Toast.makeText(mainContext,string1,Toast.LENGTH_LONG).show();
            }
        }
        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();

        AsyncTaskUploadClassOBJ.execute();
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
        } catch (Exception e) {
            return "";
        }
    }
}
