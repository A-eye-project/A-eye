package com.example.a_eye;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.a_eye.Audio.TTSAdapter;
import com.example.a_eye.Camera.Camera_Fragment;
import com.example.a_eye.Support.ForegroundService;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 5469;
    public static boolean activity_die;
    //서비스 변수
    private Intent serviceIntent;

    public static TTSAdapter myspeaker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("status","onCreate");
        super.onCreate(savedInstanceState);
        checkPermission();
        activity_die = false;
        // 핸드폰 상단 시스템바 투명하게 해주는 부분
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        if(myspeaker == null) {
            Log.i("status","make New");
            myspeaker = new TTSAdapter(this);
        }
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera_Fragment.newInstance(this))
                    .commit();
        }

        setContentView(R.layout.activity_main);
    }

    private void makeToast(String msg) {
        makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    //뒤로가기 버튼 눌렀을 때
    // 마지막으로 뒤로 가기 버튼을 눌렀던 시간 저장
    private long backKeyPressedTime = 0;
    // 첫 번째 뒤로 가기 버튼을 누를 때 표시

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            makeToast("뒤로 가기 버튼을 한 번 더 누르시면 종료됩니다.");
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
        }
    }
    @Override
    public void onStop(){
        super.onStop();
        Log.i("here","onStop");
        if (ForegroundService.serviceIntent!=null){
            activity_die = true;
            ForegroundService.cm.onSetup();
        }
    }

    @Override
    public void onDestroy(){
        Log.i("here","Destroy");
        if (myspeaker.tts != null) {
            myspeaker.stop();
            myspeaker.ttsShutdown();
            myspeaker = null;
        }
        super.onDestroy();
    }
    public void start_service(){
        if (ForegroundService.serviceIntent==null) {
            startService(new Intent(this, ForegroundService.class));
        } else {
            serviceIntent = ForegroundService.serviceIntent;//getInstance().getApplication();
            Toast.makeText(getApplicationContext(), "already", Toast.LENGTH_LONG).show();
        }

    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 다른앱 위에 그리기 체크
                Uri uri = Uri.fromParts("package" , getPackageName(), null);
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                start_service();
            }
        } else {
            start_service();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                finish();
            } else {
                start_service();
            }
        }
    }
}