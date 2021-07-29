package com.example.A_eye_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import static android.speech.tts.TextToSpeech.ERROR;
import com.example.A_eye_demo.support.Data_storage;
import com.example.A_eye_demo.support.TTSAdapter;

import java.io.File;
import java.util.Locale;

public class Image_view extends AppCompatActivity {
    ImageView image_view;
    TextView textViewv;
    private TTSAdapter tts = null; //TTS 사용하고자 한다면 1) 클래스 객체 선언
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_image_view);
        image_view = (ImageView)findViewById(R.id.imageView);
        textViewv = (TextView)findViewById(R.id.result_text);
        image_view.setImageBitmap(Data_storage.img);
        Data_storage.Flag=false;
        textViewv.setText(Data_storage.ttxString); //보여주기
        tts = TTSAdapter.getInstance(this);
    }
    @Override
    public void onStart(){
        super.onStart();
        /*Handler n = new Handler();
        n.postDelayed(new Runnable() {
            @Override
            public void run() {
                tts.speak(Data_storage.ttxString);// 말하기
            }
        },2000); //*/
        tts.speak(Data_storage.ttxString);// 말하기
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
    }


}