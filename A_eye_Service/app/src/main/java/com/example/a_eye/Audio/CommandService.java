package com.example.a_eye.Audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.a_eye.MainActivity;
import com.example.a_eye.R;
import com.example.a_eye.Support.Global_variable;

public class CommandService extends Service {
    // 시작 확인 변수
    public static boolean isStarted = false;

    // 음성인식
    private Command command;

    public CommandService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("서비스", "onCreate");
        super.onCreate();

        // Command 생성
        command = new Command(getApplicationContext());
        command.onSetup();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("서비스", "onStartCommand");
        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
                case Global_variable.ACTION.START_FOREGROUND:
                    sendNotification();
                    // showNotification();
                    break;

                case Global_variable.ACTION.STOP_FOREGROUND:
                    command.cancel();
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        command.cancel();
    }

    private void sendNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Global_variable.ACTION.MAIN_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // 버튼 추가
        Intent buttonCloseIntent = new Intent(this, CommandService.class);
        buttonCloseIntent.setAction(Global_variable.ACTION.STOP_FOREGROUND);
        PendingIntent pCloseIntent =
                PendingIntent.getService(this, 0, buttonCloseIntent, 0);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, Global_variable.NOTIFICATION.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)//drawable.splash)
                        .setContentTitle("Service test")
                        .setContentText("실행중")
                        .setContentIntent(pendingIntent) //
                        .setOngoing(true)           // 사용자가 알림을 못지움
                        .setSound(defaultSoundUri)  // 등장시 소리
                        .addAction(R.drawable.ic_launcher_foreground, "Close", pCloseIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(Global_variable.NOTIFICATION.CHANNEL_ID,"Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(Global_variable.NOTIFICATION.FOREGROUND_SERVICE, notificationBuilder.build());
    }
}