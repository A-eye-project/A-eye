package com.example.A_eye_demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.TTSAdapter;

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
    // 시간조정
    TextView tv;
    TotalClass total = new TotalClass();
    private TTSAdapter tts = null; //TTS 사용하고자 한다면 1) 클래스 객체 선언

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.caption_id);
        Data_storage.Flag = Boolean.FALSE;

        total.setup(this, tv);

        if (permissionCheck() == false) {
            tts.speak("어플을 이용하기 위한 권한을 모두 허용해 주세요.");
            permissionCheck();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tts.ttsShutdown();
        if(total.get_pocket_isalive() == true) total.cancel();
    }

    public boolean permissionCheck() {
        //권한이 허용되어 있는지 확인한다.
        String tmp = "";

        //카메라 권한 확인 > 권한 승인되지 않으면 tmp에 권한 내용 저장
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.CAMERA+" ";
        }

        //카메라 저장 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
        }

        //음성 녹음 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.RECORD_AUDIO+" ";
        }

        //tmp에 내용물이 있다면, 즉 권한 승인받지 못한 권한이 있다면
        if(TextUtils.isEmpty(tmp) == false) {
            //권한 요청하기
            tts.speak("어플을 이용하기 위해 화면에 뜨는 모든 권한을 허용해 주세요.");
            ActivityCompat.requestPermissions(this, tmp.trim().split(" "), 1);
            return false;
        } else {
            //허용 되어 있으면 그냥 두기
            Log.d("상황: ", "권한 모두 허용");
            return true;
        }
    }

    //권한에 대한 응답이 있을때 자동 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //권한 허용했을 경우
        if (requestCode == 1) {
            int length = permissions.length;
            for (int i = 0; i < length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onResume(){
        super.onResume();
        if (Data_storage.Flag == true) {
            total.resume(this);
        } else {
            if (permissionCheck() == true) { total.set_pocket_flag(true); }
            if (total.get_pocket_flag() == true) tts.speak("'adam'이라고 말해주세요");// 말하고
            total.pocket_set();
        }
    }
}