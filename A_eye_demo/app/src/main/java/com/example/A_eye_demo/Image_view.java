package com.example.A_eye_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import static android.speech.tts.TextToSpeech.ERROR;
import com.example.A_eye_demo.support.Data_storage;

import java.io.File;
import java.util.Locale;

public class Image_view extends AppCompatActivity {
    ImageView image_view;
    TextView textViewv;
    Bitmap myBitmap;
    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_image_view);
        image_view = (ImageView)findViewById(R.id.imageView);
        textViewv = (TextView)findViewById(R.id.result_text);
        File files = new File(Data_storage.img_path);
        if(files.exists()==true) {
            myBitmap = BitmapFactory.decodeFile(files.getAbsolutePath());
            Data_storage.img = Rotate_bitmap(myBitmap);
            image_view.setImageBitmap(Data_storage.img);
        }
        Data_storage.Flag=false;
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status!= ERROR){
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(0.8f);
                }
            }
        });

        //tts.speak(res, Data_storage.ttxString.QUEUE_FLUSH, null);
    }

    public Bitmap Rotate_bitmap(Bitmap myimg){
        Matrix matrix = new Matrix();
        //이미지의 각도를 90 주는 이유
        //Preview 에서는 1280 * 720(반대로 720*1280 지원하지 않음.)을 지원하기 때문에,
        //해당 이미지의 경우 90를 회전 시켜준다.
        matrix.postRotate(90);

        //새롭게 만들어진 bitmap으로 이미지 파일을 생성한다.
        Bitmap new_bit =  Bitmap.createBitmap(myimg, 0, 0, myimg.getWidth(), myimg.getHeight(), matrix, true);
        return new_bit;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Data_storage.Flag=false;
    }


}