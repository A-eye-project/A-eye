package com.example.A_eye_demo;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.A_eye_demo.Record.Record;
import com.example.A_eye_demo.support.Choice;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class pocketsphinx implements RecognitionListener {
    // 활성 키워드
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "adam";

    //변수
    private Handler mHandler;
    public boolean isalive = false; // setup 여부
    public boolean Flag = false; // 권한 설정 완료 됐는지 여부.
    String Result;

    // 디코더
    private SpeechRecognizer recognizer;

    private Context myContext;
    private TextView myText;
    pocketsphinx(Context context, TextView tv) {
        myContext = context;
        myText = tv;
    }

    public void onSetup() {
        isalive = true;
        new setupTask(this).execute();
    }

    private static class setupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<pocketsphinx> activityReference;

        setupTask(pocketsphinx activity) {
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
                activityReference.get().makeToast("No file");
            } else {
                activityReference.get().recognizer.startListening(KWS_SEARCH);
            }
        }

    }

    private void makeToast(String msg) {
        makeText(myContext.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            //makeToast("Found");
            recognizer.stop();
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            get_record_string();
        }
    }

    // 음성 인식이 가능한 순간을 표시해줌
    @Override
    public void onBeginningOfSpeech() {}

    // 디코더에 stop 함수가 실행되면 이곳으로 옴
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            makeToast("Fuck");
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
        makeToast("Error");
    }

    @Override
    public void onTimeout() {}

    public void cancel() {
        Flag = false;
        recognizer.cancel();
        recognizer.shutdown();
    }

    private void get_record_string() {
        Record myRecord = new Record();
        myText.setText("Recording.. ");

        new Thread(new Runnable() { //새 Thread에서 녹음 시작
            public void run() {
                try {
                    myRecord.Start_record();
                } catch (RuntimeException e) {
                    Log.i("Error", e.getMessage());
                    return;
                }
            }
        }).start();

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() { // 녹음 하는 시간 지연
            @Override
            public void run() {
                myRecord.Stop_record(); // 녹음 종료
                myText.setText("Reconizing.. ");
                int status = myRecord.net_com(); // 녹음 파일 -> String으로 바꾸는 API 통신 , return값은 통신 상태

                if (status == 1) {
                    Result = myRecord.get_re();
                    myText.setText(Result);
                    get_num();
                } else {
                    if (status == -2) {
                        myText.setText("No response from server for 20 secs");
                    } else {
                        myText.setText("Interrupted");
                    }
                }
            }
        },3000); // 녹음 시간 -> 현재 3초
    }

    private void get_num() {
        Choice my = new Choice();
        my.Set_str(" "+ Result.replaceAll(" ",""));
        my.Local_Alignment();
        int c = my.info();

        if (c == 0) {
            myText.setText("OCR");
        } else {
            if (c == 1) {
                myText.setText("ImageCaptioning");
            } else {
                myText.setText("VQA");
            }
        }

        isalive = false;
        Intent intent = new Intent(myContext.getApplicationContext(), CameraActivity.class);
        myContext.startActivity(intent);
    }

    public String get_keyword() { return KEYPHRASE; } // return Voice keyword
}