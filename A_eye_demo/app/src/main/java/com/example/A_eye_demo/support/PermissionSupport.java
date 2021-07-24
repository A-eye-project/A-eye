package com.example.A_eye_demo.support;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.A_eye_demo.Camera.Camera_Fragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionSupport {

    public Context context;
    public Activity activity;

    private String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA
    };

    private List PermissionList;

    private final int MULTIPLE_PERMISSIONS = 1202;
    /*
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 100;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 200;

    private static final int PERMISSIONS_REQUEST_INTERNET = 300;

    private static final int PERMISSIONS_REQUEST_CAMERA = 400;
*/
    public PermissionSupport(Activity act, Context ct){
        this.activity = act;
        this.context = ct;
    }

    public boolean checkPermission(){
        int result;
        PermissionList = new ArrayList<>();

        for(String pm : permissions){
            result = ContextCompat.checkSelfPermission(context, pm);
            if(result != PackageManager.PERMISSION_GRANTED){
                PermissionList.add(pm);
            }
        }
        if(!PermissionList.isEmpty()){
            return false;
        }
        return true;
    }
    public void requestPermission(){
        ActivityCompat.requestPermissions(activity, (String[]) PermissionList.toArray(new String[PermissionList.size()]), MULTIPLE_PERMISSIONS);
    }


    public boolean permissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
        if (requestCode == MULTIPLE_PERMISSIONS && (grantResults.length > 0)) {
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i] == -1){
                    return false;
                }
            }
        }
        return true;
    }

}
