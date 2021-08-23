package com.example.a_eye;


import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;


import com.example.a_eye.Audio.Command;

public class ForegroundService extends Service {
    public static Intent serviceIntent = null;
    public static boolean recording_isalive = false;
    PendingIntent pendingIntent;
    private Thread mainThread;
    Command cm = new Command(this);
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        serviceIntent = intent;
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean run = true;
                while (run) {
                    try {
                        Thread.sleep(100 *60 * 1); // 5 second
                    } catch (InterruptedException e) {
                        run = false;
                        e.printStackTrace();
                    }
                }
            }
        });
        mainThread.start();
        return START_NOT_STICKY;
    }

    //서비스가 종료될 때 할 작업


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        serviceIntent = null;
        Thread.currentThread().interrupt();

        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }
    public void showToast(final Application application, final String msg) {
        Handler h = new Handler(application.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(application, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = "fcm_default_channel";//getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)//drawable.splash)
                        .setContentTitle("Service test")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,"Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
    public void call_main(){
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(Integer.MAX_VALUE, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

            ActivityManager.RecentTaskInfo recentTaskInfo = null;

            for (int i = 0; i < recentTasks.size(); i++)
            {
                Log.i("pack_name",recentTasks.get(i).baseIntent.getComponent().getPackageName());
                if (recentTasks.get(i).baseIntent.getComponent().getPackageName().equals("com.example.a_eye")) {
                    recentTaskInfo = recentTasks.get(i);
                    break;
                }
            }

            if(recentTaskInfo != null && recentTaskInfo.id > -1) {
                MainActivity.activity_die = false;
                mainThread.interrupt();
                activityManager.moveTaskToFront(recentTaskInfo.persistentId, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }*/
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

    }

}
