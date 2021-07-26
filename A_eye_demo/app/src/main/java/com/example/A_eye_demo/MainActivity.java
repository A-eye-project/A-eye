package com.example.A_eye_demo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.ImageCaptioning;
import com.example.A_eye_demo.support.OCR;
import com.example.A_eye_demo.support.PermissionSupport;
import com.example.A_eye_demo.support.VQA;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static android.widget.Toast.makeText;


/*
전체적인 과정
1. "adam" 호출
2. 3초간 음성 명령
3. Texture View 실행
4. Camera  Open후 3초뒤 촬영  (pictureHandler로 시간 조절), 내부 저장소에 .jpg형태로 사진 저장
5. 이미지 경로 Data_Storage에 저장
6. MainActivity에서 Data_Storage의 Flag 확인
7. Flag == true일 경우 Image_view에서 촬영했던 이미지 보여줌.
8. 서버 통신 시작
9. 서버 통신 제발 끝나면 intent로 image_view 실행
10. image_view에서 이미지 보여주기 및
8. main 으로 돌아오면 1번 부터 다시 시작.
 */
public class MainActivity extends AppCompatActivity {
    // 안드로이드 기능 변수
    private pocketsphinx pocketTEST;
    TextView tv;
    int count_time;
    private PermissionSupport permission;
    private int count2;
    // 이미지 업로드 변수
    ProgressDialog progressDialog;
    private Handler mHandler;
    ImageCaptioning imgCaptioning= new ImageCaptioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();
    private boolean isSending = true;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.caption_id);
        // 권한 부여 확인
        permissionCheck();
        count_time = 0;
        count2 = 0;
        // 인식기 초기화
        pocketTEST = new pocketsphinx(this, tv);

    }

    public void permissionCheck(){
        if(Build.VERSION.SDK_INT >= 23){
            permission = new PermissionSupport(this,this);
            if(!permission.checkPermission()){
                permission.requestPermission();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (!permission.permissionsResult(requestCode,permissions,grantResults)){
            makeToast("권한설정을 완료해 주세요");
            permission.requestPermission();
        }
    }
    private void makeToast(String msg) {
        makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onStart(){
        count_time += 1;
        if(count_time == 10) finish();
        super.onStart();
        if(permission.checkPermission()) {
            // 비동기 테스크 이용
            pocketTEST.onSetup();
        }
        else{
            Handler n = new Handler();
            n.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onStart();
                }
            },1000); //
        }
    }
    @Override
    public void onRestart(){
        super.onRestart();
        if(Data_storage.Flag == true){
            //ImageUploadToServer();
            //if(Data_storage.ttxString != ""){
                Intent intent = new Intent(getApplicationContext(), Image_view.class);
                startActivity(intent);
            /*}
            else{
                Log.i("E","ServerError");
                onRestart();
            }*/

        }
        else{
            tv.setText("Tell me one more 'adam'");
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(permission.checkPermission()) pocketTEST.cancel();
    }

    public void ImageUploadToServer(){

        class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

            @Override
            protected void onPreExecute() {

                super.onPreExecute();

                // Showing progress dialog at image upload time.
                progressDialog = ProgressDialog.show(MainActivity.this, "Image is Uploading", "Please Wait", false, false);
            }

            @Override
            protected void onPostExecute(String string1) {

                super.onPostExecute(string1);

                // Dismiss the progress dialog after done uploading.
                progressDialog.dismiss();

                // Printing uploading success message coming from server on android app.
                Toast.makeText(MainActivity.this,string1,Toast.LENGTH_LONG).show();
                isSending = false;
                // Setting image as transparent after done uploading.
                //ImageViewHolder.setImageResource(android.R.color.transparent);


            }

            @Override
            protected String doInBackground(Void... params) {

                String res = "";

                if (Data_storage.choice == 0) {
                    String img = BitmapToString(Data_storage.img);
                    res = ocr.getOcrRes(img);
                }
                else if (Data_storage.choice == 1) {
                    res = imgCaptioning.getCaption(Data_storage.img);
                }
                else{
                    String img = BitmapToString(Data_storage.img);
                    res = vqa.getAns(img,Data_storage.question);
                }
                if(res != ""){
                    Data_storage.ttxString = res;
                }
                return res;
            }
        }
        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();

        AsyncTaskUploadClassOBJ.execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String BitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);
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