package com.example.a_eye.Server;

import android.graphics.Bitmap;

import com.example.a_eye.Support.Global_variable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class Image_Captioning {
    private String requestURL = "";

    public String getCaption() {

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

            Global_variable.img.compress(Bitmap.CompressFormat.JPEG, 100, outPutStream);

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

        } catch (ConnectException e){
            return "Fail To Connect";
        }
        catch (Exception e) {
            return "Fail To Connect";
        }
        //Toast.makeText(MainActivity.this, Integer.toString(c),Toast.LENGTH_LONG).show();
        return stringBuilder.toString();
    }
}