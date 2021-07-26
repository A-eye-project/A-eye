package com.example.A_eye_demo.support;

import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ImageCaptioning {

    private String requestURL = "URL of Server";

    public String getCaption(Bitmap bitmap) {

        StringBuilder stringBuilder = new StringBuilder();
        try {

            URL url;
            HttpURLConnection httpURLConnectionObject ;
            OutputStream outPutStream;
            BufferedWriter bufferedWriterObject ;
            BufferedReader bufferedReaderObject ;
            int RC;
            url = new URL(requestURL);
            httpURLConnectionObject = (HttpURLConnection) url.openConnection();
            httpURLConnectionObject.setReadTimeout(19000);
            httpURLConnectionObject.setConnectTimeout(19000);
            httpURLConnectionObject.setRequestMethod("POST");
            httpURLConnectionObject.setDoInput(true);
            httpURLConnectionObject.setDoOutput(true);
            outPutStream = httpURLConnectionObject.getOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outPutStream);

            outPutStream.close();
            RC = httpURLConnectionObject.getResponseCode();
            if (RC == HttpsURLConnection.HTTP_OK) {

                bufferedReaderObject = new BufferedReader(new InputStreamReader(httpURLConnectionObject.getInputStream()));

                stringBuilder = new StringBuilder();

                String RC2;

                while ((RC2 = bufferedReaderObject.readLine()) != null){

                    stringBuilder.append(RC2);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //Toast.makeText(MainActivity.this, Integer.toString(c),Toast.LENGTH_LONG).show();
        return stringBuilder.toString();
    }
}