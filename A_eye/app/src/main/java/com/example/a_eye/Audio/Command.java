package com.example.a_eye.Audio;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.example.a_eye.MainActivity;
import com.example.a_eye.Support.Global_variable;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static androidx.core.content.ContextCompat.startActivity;

public class Command implements RecognitionListener {
    // 활성 키워드
    public static final String KWS_SEARCH = "wakeup";
    private String KEYPHRASE;
    //변수
    public static boolean startFunction;
    public static boolean isalive;


    // 진동 객체
    private Vibrator vibrator;


    private Handler mHandler = new Handler();

    // 디코더
    public static SpeechRecognizer recognizer;

    private Context myContext;

    public Command(Context context, String key) {
        isalive = false;
        myContext = context;
        vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        KEYPHRASE = key;
    }

    public void onSetup() {
        Log.i("mycommand", KEYPHRASE);
        new setupTask(this).execute();
        startFunction = false;
        isalive = true;
    }

    public void StartListening() {
        recognizer.startListening(KWS_SEARCH);
    }

    private static class setupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<Command> activityReference;

        setupTask(Command activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                Assets assets = new Assets(activityReference.get().myContext);
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                Log.i("error","No file");
            } else {
                activityReference.get().recognizer.startListening(KWS_SEARCH);
            }
        }

    }


    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            stop();
            command_vive();
            launch_fun();
        }
    }

    private void launch_fun() { // (확인)
        if (Global_variable.behind_app) {
                /*
                 어플리케이션이 감춰진 상태에서 부르는 상황 고로
                 인식이 되면 새로운 액티비티를 불러오고 동작후 액티비티 종료하면 됨.
                 */
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    call_main();
                }
            }, 500);
        } else {
                /*
                어플이 전면에 나와있는 상태에서 실행 바로 음성 녹음과 동작 실행하면 됨.
                 */
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startFunction = true;
                }
            }, 500);
        }
    }

    private void command_vive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(300,10));
        } else {
            vibrator.vibrate(300);
        }
    }

    public void call_main(){
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            final ActivityManager activityManager = (ActivityManager) myContext.getSystemService(Context.ACTIVITY_SERVICE);
            final List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(Integer.MAX_VALUE, ActivityManager.RECENT_IGNORE_UNAVAILABLE);

            ActivityManager.RecentTaskInfo recentTaskInfo = null;

            for (int i = 0; i < recentTasks.size(); i++)
            {
                Log.i("pack_name",recentTasks.get(i).baseIntent.getComponent().getPackageName());
                if (recentTasks.get(i).baseIntent.getComponent().getPackageName().contains("com.example.a_eye")) {
                    recentTaskInfo = recentTasks.get(i);
                    break;
                }
            }

            if(recentTaskInfo != null && recentTaskInfo.id > -1) {
                MainActivity.activity_die = false;
                activityManager.moveTaskToFront(recentTaskInfo.persistentId, ActivityManager.MOVE_TASK_WITH_HOME);

                return;
            }
        } */


        Log.i("start","call_main");
        Intent intent = new Intent(myContext, MainActivity.class);
        startActivity(myContext,intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),null);
    }
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
        }
    }

    // 음성 인식이 가능한 순간을 표시해줌
    @Override
    public void onBeginningOfSpeech() {
    }

    // 디코더에 stop 함수가 실행되면 이곳으로 옴
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            ;
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir)
                .setKeywordThreshold(1e-10f)
                .getRecognizer();

        recognizer.addListener(this);
        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    @Override
    public void onError(Exception error) {
        Log.i("error","Error");
    }

    @Override
    public void onTimeout() {
    }

    public void stop() {
        isalive = false;
        recognizer.stop();
    }

    public void cancel() {
        Log.i("mycommand_cancle",KEYPHRASE);
        recognizer.cancel();
        recognizer.shutdown();
    }

}