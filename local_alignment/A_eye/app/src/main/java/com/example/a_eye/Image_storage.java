package com.example.a_eye;

import android.app.Application;
import android.graphics.Bitmap;

public class Image_storage extends Application {
    public static Bitmap img;

    public static void setImg(Bitmap img) {
        Image_storage.img = img;
    }

    public static Bitmap getImg() {
        return img;
    }
}
