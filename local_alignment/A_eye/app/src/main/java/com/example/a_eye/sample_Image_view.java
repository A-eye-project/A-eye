package com.example.a_eye;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class sample_Image_view extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_image_view);
        ImageView image_view = (ImageView)findViewById(R.id.imageView);
        image_view.setImageBitmap(Image_storage.img);
    }
}
