package com.example.a_eye_demo.Audio;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a_eye_demo.MainActivity;
import com.example.a_eye_demo.Support.Select_Function;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class Command implements RecognitionListener {
    // 활성 키워드
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "base";
    //변수
    public static boolean startact;
    public static boolean isalive;
    String Result;

    // 디코더
    private SpeechRecognizer recognizer;

    private Context myContext;

    public Command(Context context) {
        myContext = context;
    }

    public void onSetup() {
        isalive = true;
        new setupTask(this).execute();
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
                Log.i("now","No file");
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

            recognizer.stop();
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            startact = true;
        }
    }

    // 음성 인식이 가능한 순간을 표시해줌
    @Override
    public void onBeginningOfSpeech() {
    }

    // 디코더에 stop 함수가 실행되면 이곳으로 옴
    @Override
    public void onEndOfSpeech() {
        isalive = false;
        if (!recognizer.getSearchName().equals(KWS_SEARCH)) Log.i("e", "aa");
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
        Log.i("now","Error");
    }

    @Override
    public void onTimeout() {
        isalive = false;
    }
    public void stop(){
        isalive = false;
        recognizer.stop();
    }
    public void cancel() {
        isalive = false;
        recognizer.cancel();
        recognizer.shutdown();
    }



}