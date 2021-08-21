package com.example.a_eye_demo.Server;

import android.util.Log;

import com.example.a_eye_demo.Support.Global_variable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class VQA {

    private String requestURL = "";

    public String getAns() {

        StringBuilder stringBuilder = new StringBuilder();
        try {

            URL url;
            HttpURLConnection httpURLConnectionObject ;
            InputStream inputStream = null;
            OutputStream outPutStream;
            /*
            String jsonString;

            JSONObject jsonObject = new JSONObject();
            Global_variable.jsonObject.accumulate("Image", Global_variable.imgString);
            Global_variable.jsonObject.accumulate("Question", Global_variable.question);

            Global_variable.jsonObject.accumulate("Image",Global_variable.imgString);
            Global_variable.jsonObject.accumulate("Question", Global_variable.question);
            // convert JSONObject to JSON to String

            jsonString = Global_variable.jsonObject.toString();
             */
            Log.i("what",Global_variable.jsonString);
            Global_variable.jsonObject = new JSONObject();



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
            outPutStream.write(Global_variable.jsonString.getBytes("euc-kr"));
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
        catch (ConnectException e){
            return "Fail To Connect";
        }
        catch (Exception e) {
            return "Fail To Connect";
        }
        //Toast.makeText(MainActivity.this, Integer.toString(c),Toast.LENGTH_LONG).show();
        return stringBuilder.toString();
    }
}
