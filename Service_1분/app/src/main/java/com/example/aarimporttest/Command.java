package com.example.aarimporttest;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static androidx.core.content.ContextCompat.startActivity;
import static java.lang.Integer.MAX_VALUE;


public class Command implements RecognitionListener {
    // 활성 키워드
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "adam";
    //변수
    public static boolean startCapture;
    public static boolean isalive;
    private Handler mHandler = new Handler();;
    String Result;
    private Vibrator vibrator;

    // 디코더
    private SpeechRecognizer recognizer;

    private Context myContext;

    public Command(Context context) {
        myContext = context;
        vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
        startCapture = false;
    }

    public void onSetup() {
        new setupTask(this).execute();
        isalive = true;
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
                Log.i("what??",Integer.toString(1000*60*60*24));
            }
        }

    }

    private static class cancle_task extends AsyncTask<Void, Void, Exception> {
        WeakReference<Command> activityReference;

        cancle_task(Command activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... voids) {
             Thread th;
            th = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean run = MainActivity.activity_die;
                    while(run){
                        try{
                            Thread.sleep(1000 * 10);
                            activityReference.get().recognizer.cancel();
                            Thread.sleep(1000);
                            activityReference.get().recognizer.startListening(KWS_SEARCH);
                        }catch (InterruptedException e) {
                            run = false;
                            e.printStackTrace();
                        }
                    }
                }
            });
            th.start();
            return null;
        }

    }


    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            recognizer.stop();
            vibrator.vibrate(500);
            call_main();
        }
    }

    public void call_main(){
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
                .setKeywordThreshold(1e-20f)
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
        Log.i("time","Timeout!!!!");
    }
    public void stop(){
        Log.i("stop","stop!!!!");
        isalive = false;
        recognizer.stop();
    }
    public void cancel() {
        recognizer.cancel();
        recognizer.shutdown();
    }




}