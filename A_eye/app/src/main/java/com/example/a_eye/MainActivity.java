package com.example.a_eye;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.a_eye.Camera.Camera_Fragment;
import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("status","onCreate");
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera_Fragment.newInstance())
                    .commit();
        }
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
            finishAndRemoveTask();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}