package com.example.A_eye_demo.support;

import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class VQA {

    private String requestURL = "URL of Server";

    public String getAns(String img, String Question) {

        StringBuilder stringBuilder = new StringBuilder();
        try {

            URL url;
            HttpURLConnection httpURLConnectionObject ;
            InputStream inputStream = null;
            OutputStream outPutStream;

            String json = "";

            //make json
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("Image", img);
            jsonObject.accumulate("Question", Question);

            // convert JSONObject to JSON to String
            json = jsonObject.toString();


            BufferedReader bufferedReaderObject ;
            int RC;

            url = new URL(requestURL);
            httpURLConnectionObject = (HttpURLConnection) url.openConnection();
            httpURLConnectionObject.setReadTimeout(19000);
            httpURLConnectionObject.setConnectTimeout(19000);
            httpURLConnectionObject.setRequestMethod("POST");
            // InputStream으로 서버로 부터 응답을 받겠다는 옵션.
            httpURLConnectionObject.setDoInput(true);
            // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
            httpURLConnectionObject.setDoOutput(true);
            // head option.
            httpURLConnectionObject.setRequestProperty("Accept", "application/json");
            httpURLConnectionObject.setRequestProperty("Content-type", "application/json");

            outPutStream = httpURLConnectionObject.getOutputStream();
            outPutStream.write(json.getBytes("euc-kr"));
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
            else{
                return "Did not work!";
            }

            }
        catch (Exception e) {
            e.printStackTrace();
        }
        //Toast.makeText(MainActivity.this, Integer.toString(c),Toast.LENGTH_LONG).show();
        return stringBuilder.toString();
    }

}
