package com.example.a_eye;

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

import androidx.appcompat.app.AppCompatActivity;

import com.example.a_eye.Audio.CommandService;
import com.example.a_eye.Camera.Camera_Fragment;
import com.example.a_eye.Support.Global_variable;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE= 5469;
    public static boolean activity_die;

    //서비스 변수
    public static Intent serviceIntent;

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

        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera_Fragment.newInstance(this))
                    .commit();
        }

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume(){
        super.onResume();
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
            if (CommandService.isStarted) stopService(serviceIntent);
            finish();
        }
    }

    @Override
    public void onStop(){
        super.onStop();

        // 어플리케이션이 숨겨진 경우
        Global_variable.behind_app = true;
        Log.d("메인", "onStop Behind 값 " + Global_variable.behind_app);
    }

    @Override
    protected void onRestart() {
        Log.d("메인","onRestart");
        super.onRestart();

        // 어플리케이션이 숨겨져 있다가 다시 전면으로 나옴.
        Global_variable.behind_app = false;
        Log.d("메인", "onRestart Behind 값 " + Global_variable.behind_app);
    }

    @Override
    public void onDestroy(){
        Log.i("here","Destroy");
        activity_die = true;
        super.onDestroy();
    }

    public void startService() {
        if (!CommandService.isStarted) {
            serviceIntent = new Intent(this, CommandService.class);
            serviceIntent.setAction(Global_variable.ACTION.START_FOREGROUND);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 이상인 경우
                this.startForegroundService(serviceIntent);
            } else { // API 26 미만인 경우
                this.startService(serviceIntent);
            }

            CommandService.isStarted = true;
            makeToast("서비스 실행 완료");
            Log.d("서비스", "서비스 실행");
        } else {
            Log.d("서비스", "이미 실행 중");
        }
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 다른앱 위에 그리기 체크
                Uri uri = Uri.fromParts("package" , getPackageName(), null);
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                startService();
            }
        } else {
            startService();
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
                startService();
            }
        }
    }
}