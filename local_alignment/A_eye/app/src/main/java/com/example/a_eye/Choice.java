package com.example.a_eye;

import android.app.Application;
import android.util.Log;

import java.util.Arrays;

public class Choice extends Application {
    public static String my_str;
    public static String sample_ocr[] = {" 읽어줘"," 읽어주세요"," 글자읽어줘"};
    public static String sample_image[] = {" 이그림설명해줘", " 사진내용알려줘"," 사진내용이뭐야"};
    public static String Sample [][] = {sample_ocr,sample_image};
    public static int Score [] = {0,0};
    public static int num = -1;
    @Override
    public void onCreate(){
        super.onCreate();
        my_str = "";
    }

    public void Set_str(String str){
        str = str.replace("?","");
        str = str.replace("!","");
        str = str.replace(".","");

        my_str = str;
        Log.i("test",my_str);
    }
    public void Local_Alignment(){
        Score[0] = 0;
        Score[1] = 0;
        int m = my_str.length();
        int match = 100/m;
        int miss_match = -5;
        int gap = -5;
        String x = my_str;
        for(int T=0;T<Sample.length;++T){
            for(int s = 0 ; s < Sample[T].length; ++s){
                String y = Sample[T][s];
                int i,j;
                int n = y.length();
                //Log.i("len",Integer.toString(m) + ", " + Integer.toString(n));
                Log.i("str",x + ", " + y);
                int dp[][] = new int[n + m + 1][n + m + 1];
                for (int[] x1 : dp)
                    Arrays.fill(x1,0);

                for(i=0;i<=(n+m);++i){
                    dp[i][0] = i*gap;
                    dp[0][i] = i*gap;
                }
                for(i=1;i<m;++i) {
                    for (j = 1; j <n; ++j) {
                        /*
                        if (x.charAt(i - 1) == y.charAt(j - 1)) {
                            dp[i][j] = dp[i - 1][j - 1];
                        } else {
                            dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + match,
                                    dp[i - 1][j] + gap),
                                    dp[i][j - 1] + gap);
                        }*/

                        //Log.i("count", Integer.toString(i) + ", " + Integer.toString(j));
                        if(x.charAt(i) == y.charAt(j)) {
                            dp[i][j] += Math.max(
                                    dp[i - 1][j - 1] + match, Math.max(dp[i][j - 1] + gap,dp[i - 1][j] + gap));
                        }
                        else {
                            dp[i][j] += Math.max(
                                    dp[i - 1][j - 1] + miss_match, Math.max(dp[i][j - 1] + gap,dp[i - 1][j] + gap));
                        }
                    }
                }
                Log.i("score",Integer.toString(dp[m-1][n-1]) + ", " + Integer.toString(dp[m-1][n-1]));
                Score[T] = Math.max(Score[T], dp[m-1][n-1]);
            }
        }
    }
    public int info(){
        if(Math.max(Score[0],Score[1]) > 50){
            if(Score[0] > Score[1]){
                num = 0;
            }
            else{
                num = 1;
            }
        }

        Log.i("my_score",Integer.toString(Score[0]) + ", " + Integer.toString(Score[1]));

        if(num == -1){
            num = 2;
        }
        return num;
    }
}
