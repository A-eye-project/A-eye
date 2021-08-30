package com.example.a_eye.Support;

import android.app.Application;
import android.util.Log;
import java.util.Arrays;

public class Select_Function  extends Application {
    public static String my_str;
    public static String sample_image[] = {" 설명해", " 묘사해", " 말해","알려","알려"};
    public static int Score_image;
    public static boolean OCR_Flag;
    public static int num;
    @Override
    public void onCreate(){
        super.onCreate();
        my_str = "";
        num = -1;
        Score_image = 0;
        OCR_Flag = false;
    }

    public void Set_str(String str) {
        str = str.replace("?","");
        str = str.replace("!","");
        str = str.replace(".","");

        my_str = str;
        Log.i("test",my_str);
    }

    public void Local_Alignment() {
        Score_image = 0;
        OCR_Flag = false;
        String x = my_str;
        if (x.contains("읽어")) {// OCR
            OCR_Flag = true;
        } else { // Get Image Captioning Score
            int m = my_str.length();
            int match = 20;
            int miss_match = -2;
            int gap = -2;
            for (int s = 0 ; s < sample_image.length; ++s) {
                String y = sample_image[s];
                int i,j;
                int n = y.length();
                //Log.i("len",Integer.toString(m) + ", " + Integer.toString(n));
                Log.i("str",x + ", " + y);
                int dp[][] = new int[n + m + 1][n + m + 1];
                for (int[] x1 : dp)
                    Arrays.fill(x1,0);

                for (i = 0; i <= (n + m); ++i) {
                    dp[i][0] = i*gap;
                    dp[0][i] = i*gap;
                }

                for (i = 1; i < m; ++i) {
                    for (j = 1; j <n; ++j) {
                        if (x.charAt(i) == y.charAt(j)) {
                            dp[i][j] += Math.max(
                                    dp[i - 1][j - 1] + match, Math.max(dp[i][j - 1] + gap,dp[i - 1][j] + gap));
                        } else {
                            dp[i][j] += Math.max(
                                    dp[i - 1][j - 1] + miss_match, Math.max(dp[i][j - 1] + gap,dp[i - 1][j] + gap));
                        }
                    }
                }
                Score_image = Math.max(Score_image, dp[m-1][n-1]);
            }
        }

        if (OCR_Flag == true) { // OCR
            num = 0;
        } else if(Score_image > 30) { // Image captioning
            num = 1;
        } else { // VQA
            num = 2;
        }
        Global_variable.question = my_str;
        Global_variable.choice = num;
    }
}
