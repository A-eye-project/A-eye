package com.example.A_eye_demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.PermissionSupport;

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
8. main 으로 돌아오면 1번 부터 다시 시작.
 */
public class MainActivity extends AppCompatActivity {
    private pocketsphinx pocketTEST;
    TextView tv;
    int count_time;
    // 마이크 권한
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private PermissionSupport permission;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.caption_id);
        // 권한 부여 확인
        permissionCheck();
        count_time = 0;
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
        //Log.i("log","What the hell!!!!!!!!!!!!!!!!!");
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
            Intent intent = new Intent(getApplicationContext(), Image_view.class);
            startActivity(intent);
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
}