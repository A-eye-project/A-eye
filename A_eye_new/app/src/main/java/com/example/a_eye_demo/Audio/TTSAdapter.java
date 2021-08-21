package com.example.a_eye_demo.Audio;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.a_eye_demo.MainActivity;

import java.util.Locale;


//TTS를 편하게 사용하기 위한 TTSAdapter 클래스
public class TTSAdapter extends AppCompatActivity implements TextToSpeech.OnInitListener {
    public static TextToSpeech tts; //TTS 객체
    //    private String content;  //출력물
    private static TTSAdapter instance = null;

    // 생성자: private
    private TTSAdapter(Context context) {
        tts = new TextToSpeech(context, this);
    }

    // singleton pattern
    public synchronized static TTSAdapter getInstance(Context context) {
        if (instance == null) {
            instance = new TTSAdapter(context);
        }
        instance.stop();
        return instance;
    }

    //TTS 객체를 생성하면 호출되는 메소드
    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) { //TTS의 상태가 정상이라면
            tts.setLanguage(Locale.KOREAN); //언어-한국어 설정
            tts.setPitch(0.8f); //음성 톤 (1.0f 기본)
            tts.setSpeechRate(1.0f); //읽는 속도 (1.0f 기본)
        }
    }

    public void speak(String content) {
        tts.stop();
        tts.speak(content, TextToSpeech.QUEUE_FLUSH, null, null); //음성을 출력

    }
    public void stop() {
        tts.stop();
    }

    //TTS 마무리 짓기
    protected void finalize() {
        tts.shutdown();
    }

    public void ttsShutdown(){
        tts.shutdown();
    }


}