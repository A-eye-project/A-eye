package com.example.a_eye.Audio;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

//TTS를 편하게 사용하기 위한 TTSAdapter 클래스
public class TTSAdapter extends AppCompatActivity implements TextToSpeech.OnInitListener {
    public static TextToSpeech tts; //TTS 객체
    private static TTSAdapter instance = null;

    public TTSAdapter(Context context) {
        tts = new TextToSpeech(context, this);
    }

    //TTS 객체를 생성하면 호출되는 메소드
    @Override
    public void onInit(int status) {
        Log.i("status ", String.valueOf(status));
        if (status != TextToSpeech.ERROR) { //TTS의 상태가 정상이라면
            tts.setLanguage(Locale.KOREAN); //언어-한국어 설정
            tts.setPitch(0.8f);             //음성 톤 (1.0f 기본)
            tts.setSpeechRate(1.0f);        //읽는 속도 (1.0f 기본)
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
}