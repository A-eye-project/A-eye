package com.example.A_eye_demo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


/*
전체적인 과정
ㄷ
1. "adam" 호출
2. 3초간 음성 명령
3. Texture View 실행
4. Camera  Open후 3초뒤 촬영  (pictureHandler로 시간 조절)
5. 이미지 경로 Data_Storage에 저장
6. MainActivity에서 Data_Storage의 Flag 확인
7. Flag == true일 경우 Image_view에서 촬영했던 이미지 보여줌.
8. main 으로 돌아오면 1번 부터 다시 시작.
 */
public class MainActivity extends AppCompatActivity {
    private pocketsphinx pocketTEST;
    TextView tv;
    // 마이크 권한
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.caption_id);
        // 권한 부여 확인
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // 인식기 초기화
        // 비동기 테스크 이용
        pocketTEST = new pocketsphinx(this, tv);
        pocketTEST.onSetup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pocketTEST.onSetup();
            } else {
                finish();
            }
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
            pocketTEST.onSetup();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        pocketTEST.cancel();
    }

}