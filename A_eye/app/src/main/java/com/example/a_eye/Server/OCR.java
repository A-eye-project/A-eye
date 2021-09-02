package com.example.a_eye.Server;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.a_eye.Support.Global_variable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.UUID;


public class OCR {
    private String ocrApiUrl = "";
    private String ocrSecretKey = "";


    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getReqMessage() {
        String requestBody = "";

        try {
            long timestamp = new Date().getTime();

            JSONObject json = new JSONObject();
            json.put("version", "V1");
            json.put("requestId", UUID.randomUUID().toString());
            json.put("timestamp", Long.toString(timestamp));
            JSONObject image = new JSONObject();
            image.put("format", "jpeg");
            image.put("name", "sample");

            image.put("data", Global_variable.imgString);

            JSONArray images = new JSONArray();
            images.put(image);
            json.put("images", images);

            requestBody = json.toString();
        } catch (Exception e) {
            System.out.println("Json Exception : " + e);
        }
        return requestBody;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String reqOcr() { // img = BitmapToString(bitmap);
        String ocrMessage = "";

        try {
            String apiURL = ocrApiUrl;
            String secretKey = ocrSecretKey;

            URL url = new URL(ocrApiUrl);

            String message = getReqMessage();
            System.out.println("##" + message);

            long timestamp = new Date().getTime();

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json;UTF-8");
            con.setRequestProperty("X-OCR-SECRET", secretKey);

            // post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(message.getBytes("UTF-8"));
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200) { // 정상 호출
                System.out.println(con.getResponseMessage());

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                con.getInputStream()));
                String decodedString;
                while ((decodedString = in.readLine()) != null) {
                    ocrMessage = decodedString;
                }
                //chatbotMessage = decodedString;
                in.close();

            } else {  // 에러 발생
                ocrMessage = con.getResponseMessage();

            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return ocrMessage;
    }

    public String getOcrRes() {
        String ocrRes = "";

        try {
            JSONObject json = new JSONObject(reqOcr());
            JSONArray images = json.getJSONArray("images");
            JSONObject images_json = images.getJSONObject(0);
            JSONArray fields = images_json.getJSONArray("fields");

            for (int i = 0; i < fields.length(); ++i) {
                ocrRes += " " + fields.getJSONObject(i).get("inferText");
                //ocrRes += fields.getJSONObject(i).get("inferText");
            }
        } catch (Exception e) {
            ;
        }
        if (ocrRes == "") {
            ocrRes = "아무 내용도 없습니다";
        }

        return ocrRes;
    }
}
