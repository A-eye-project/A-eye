package com.example.A_eye_demo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.ImageCaptioning;
import com.example.A_eye_demo.support.OCR;
import com.example.A_eye_demo.support.PermissionSupport;
import com.example.A_eye_demo.support.TTSAdapter;
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
    //Button button;
    TextView tv;
    int count_time;
    private PermissionSupport permission;
    private int count2;
    ProgressDialog progressDialog;
    private Handler mHandler;
    ImageCaptioning imgCaptioning= new ImageCaptioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();
    private TTSAdapter tts = null; //TTS 사용하고자 한다면 1) 클래스 객체 선언

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.caption_id);
        Data_storage.Flag = Boolean.FALSE;
        // 권한 부여 확인
        tts = TTSAdapter.getInstance(this);
        // 인식기 초기화
        pocketTEST = new pocketsphinx(this, tv);
        if(permissionCheck()==false){
            tts.speak("어플을 이용하기 위한 권한을 모두 허용해 주세요.");
            permissionCheck();
        }

    }
    @Override
    public void onResume(){
        super.onResume();
        Handler n = new Handler();
        n.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(Data_storage.Flag == true){

                    //if(Data_storage.ttxString != ""){
                        // 서버에서 받은 문자열은 Data_Storage에 저장돼 있음.
                        Intent intent = new Intent(getApplicationContext(), Image_view.class);
                        startActivity(intent);
                        tv.setText("");
                    //}
                    //else{
                        //.i("E","ServerError");
                        // onResume();
                    //}
                }
                else{
                    if(permissionCheck() == true){pocketTEST.Flag = true;}
                    if(pocketTEST.Flag == true) tts.speak("'adam'이라고 말해주세요");// 말하고
                    pocket_set();// 2초뒤 부터 adam 실행.
                }
            }
        },2000); //
    }
    public void pocket_set(){
        Handler n = new Handler();
        n.postDelayed(new Runnable() {
            @Override
            public void run() {
                if((pocketTEST.Flag == true) && (pocketTEST.isalive == false) ) {
                    pocketTEST.onSetup();
                    tv.setText("Tell 'adam'");
                }
                else{
                    Log.i("target","What??");
                }
            }
        },2000); //
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        tts.ttsShutdown();
        pocketTEST.cancel();
    }
    public boolean permissionCheck(){
        //권한이 허용되어 있는지 확인한다.

            String tmp = "";

            //카메라 권한 확인 > 권한 승인되지 않으면 tmp에 권한 내용 저장
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                tmp += Manifest.permission.CAMERA+" ";
            }

            //카메라 저장 권한 확인
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
            }

            //음성 녹음 권한 확인
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
                tmp += Manifest.permission.RECORD_AUDIO+" ";
            }
            //tmp에 내용물이 있다면, 즉 권한 승인받지 못한 권한이 있다면
            if(TextUtils.isEmpty(tmp) == false) {
                //권한 요청하기
                tts.speak("어플을 이용하기 위해 화면에 뜨는 모든 권한을 허용해 주세요.");
                ActivityCompat.requestPermissions(this, tmp.trim().split(" "), 1);
                return false;
            }else{
                //허용 되어 있으면 그냥 두기
                Log.d("상황: ", "권한 모두 허용");
                return true;
            }

    }
    //권한에 대한 응답이 있을때 자동 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //권한 허용했을 경우
        if(requestCode == 1){
            int length = permissions.length;
            for(int i=0; i<length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    //동의
                    Log.d("상황: ","권한 허용 "+permissions[i]);
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
    private void makeToast(String msg) {
        makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
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

                progressDialog.dismiss();

                Toast.makeText(MainActivity.this,string1,Toast.LENGTH_LONG).show();


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