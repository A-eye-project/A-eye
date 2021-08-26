package com.example.a_eye.Audio;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import com.example.a_eye.MainActivity;
import com.example.a_eye.Support.ForegroundService;
import com.example.a_eye.Support.Select_Function;
import com.example.a_eye.Support.Set_Dialog;

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
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "adam";
    //변수
    public static boolean startCapture;
    public static boolean isalive;


    private Handler mHandler = new Handler();

    // 디코더
    private SpeechRecognizer recognizer;

    private Context myContext;

    public Command(Context context) {
        myContext = context;
    }

    public void onSetup() {
        new setupTask(this).execute();
        startCapture = false;
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
            }
        }

    }


    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            ForegroundService.service = false;
            cancel();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    call_main();

                }
            },500);
        }
    }
    public void call_main(){
        Log.i("start","call_main");
        Intent intent = new Intent(myContext, MainActivity.class);
        startActivity(myContext,intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),null);
        //Catch.cancel();
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
    public void stop(){
        isalive = false;
        recognizer.stop();
    }
    public void cancel() {
        recognizer.cancel();
        recognizer.shutdown();
    }

}