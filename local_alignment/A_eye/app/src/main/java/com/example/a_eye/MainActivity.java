package com.example.a_eye;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button buttonStart,Btn_view;
    String Result;
    TextView textResult;
    private Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck();
        textResult = (TextView)findViewById(R.id.textResult);
        buttonStart = (Button)findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new  View.OnClickListener() {
            public void onClick(View v) {
                get_record_string();
            }
        });
    }
    private void permissionCheck(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void ToastMessage(String msg) {
        Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
    }
    private void get_record_string(){
        Record myRecord = new Record();
        textResult.setText("Recording.. ");
        new Thread(new Runnable() { //새 Thread에서 녹음 시작
            public void run() {
                try {
                    myRecord.Start_record();
                } catch (RuntimeException e) {
                    Log.i("Error", e.getMessage());
                    return;
                }
            }
        }).start();
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable(){ // 녹음 하는 시간 지연
            @Override
            public void run(){
                myRecord.Stop_record(); // 녹음 종료
                textResult.setText("Reconizing.. ");
                int status = myRecord.net_com(); // 녹음 파일 -> String으로 바꾸는 API 통신 , return값은 통신 상태
                if(status == 1){
                    textResult.setText("통신완료");
                    Result = myRecord.get_re();
                    ToastMessage(Result);
                    get_num();
                }
                else{
                    if(status == -2){
                        textResult.setText("No response from server for 20 secs");
                    }
                    else{
                        textResult.setText("Interrupted");
                    }
                }
            }
        },3000); // 녹음 시간 -> 현재 3초
    }
    private void get_num(){
        Choice my = new Choice();
        my.Set_str(" "+ Result.replaceAll(" ",""));
        my.Local_Alignment();
        int c = my.info();
        if(c == 0){
            textResult.setText("OCR");
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(intent);
        }
        else{
            if(c == 1){
                textResult.setText("ImageCaptioning");
            }
            else{
                textResult.setText("VQA");
            }
        }
    }
}