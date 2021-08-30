package com.example.a_eye_demo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.a_eye_demo.Camera.AutoFitTextureView;
import com.example.a_eye_demo.Server.Image_Captioning;
import com.example.a_eye_demo.Server.OCR;
import com.example.a_eye_demo.Server.VQA;
import com.example.a_eye_demo.Audio.Command;
import com.example.a_eye_demo.Support.Global_variable;
import com.example.a_eye_demo.Audio.TTSAdapter;

import org.json.JSONException;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity {


    // 카메라 뷰
    private AutoFitTextureView textureView;

    // 실행 순서 변수

    // 권한설정 완료 변수
    private boolean permission_complete;

    //지연시간 변수
    private Handler timer;

    //안드로이드 명령어 변수
    private Command command;
    //서버관련 변수
    public static  Boolean Upload_Status,Success_upload,TTs_End;
    ProgressDialog progressDialog;
    Image_Captioning imgCaptioning= new Image_Captioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();

    // tts 변수
    private TTSAdapter tts = null; //TTS 사용하고자 한다면 1) 클래스 객체 선언
    //이미지 출력 상태 확인
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    //권한 관련
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    //카메라 관련 변수들
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private String cameraId; //카메라 아이디- 어떤 카메라를 쓸 것인가?
    private CameraDevice cameraDevice; //카메라 장치
    private CameraCaptureSession cameraCaptureSessions; //사진 찍을 때 사용할 변수
    private CaptureRequest.Builder captureRequestBuilder;   //사진 찍기 요청 빌더 변수
    private Size imageDimension; //이미지 치수를 받아오고 전달하는 변수




    //비트맵 관련 변수들
    private Bitmap bitmap;
    private int rotation;
    private byte[] bytes;
    private ByteBuffer buffer;
    // 카메라 핸들러
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Size previewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("status","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 권한 부여 확인
        timer = new Handler();
        tts = TTSAdapter.getInstance(this);
        command = new Command(this);
        permission_complete = false;

        textureView = (AutoFitTextureView)findViewById(R.id.textureView);
        Upload_Status = false;
        Success_upload = false;
    }
    @Override
    protected void onStart(){
        Log.i("status","onStart");
        super.onStart();
    }

    protected void onPause() {;
        Log.i("status","onPause");
        closeCamera();
        stopBackgoundThread();
        super.onPause();
    }
    protected void onStop() {
        Log.i("status","onStop");
        super.onStop();
    }
    @Override
    protected void onResume() { // 요부분 없으면 권한 설정에서 다 깨진다.
        Log.i("status","onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        super.onResume();
    }

    private void SetupCommand(){
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                command.onSetup();
                makeToast("Tell 'adam'");
                catch_catpture();
            }
        },1000);
    }

    private void catch_catpture() { //Command에서 Localignment까지 종료시 실행.
        Timer Catch = new Timer();
        TimerTask capture = new TimerTask() {
            @Override
            public void run() {
                if(command.startCapture == true){
                    Catch.cancel();
                    takePicture();
                }
            }
        };

        Catch.schedule(capture,5000,1000);
    }

    private void catch_ttsEnd(){//Command에서 Localignment까지 종료시 실행.
        Log.i("what??",String.valueOf(tts.tts.isSpeaking()));
        Timer Catch = new Timer();
        TimerTask tts_end = new TimerTask() {
            @Override
            public void run() {
                if(tts.tts.isSpeaking() == false){
                    Catch.cancel();
                    SetupCommand();
                }
            }
        };
        Catch.schedule(tts_end,1000,1000);
    }

    private void makeToast(String msg) {
        makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    //뒤로가기 버튼 눌렀을 때
    // 마지막으로 뒤로 가기 버튼을 눌렀던 시간 저장
    private long backKeyPressedTime = 0;
    // 첫 번째 뒤로 가기 버튼을 누를 때 표시

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        // 기존 뒤로 가기 버튼의 기능을 막기 위해 주석 처리 또는 삭제

        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지났으면 Toast 출력
        // 2500 milliseconds = 2.5 seconds
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            makeToast("뒤로 가기 버튼을 한 번 더 누르시면 종료됩니다.");
            return;
        }
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간에 2.5초를 더해 현재 시간과 비교 후
        // 마지막으로 뒤로 가기 버튼을 눌렀던 시간이 2.5초가 지나지 않았으면 종료
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            secondcandle();
            makeToast("이용해 주셔서 감사합니다.");
            finish();
        }
    }
    public void secondcandle(){// 두번 뒤로가기.
        command.cancel();
        closeCamera();
        stopBackgoundThread();
        tts.ttsShutdown();
        moveTaskToBack(true);
        finishAndRemoveTask();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    public boolean permissionCheck(){
        //권한이 허용되어 있는지 확인한다.
        String tmp = "";

        //카메라 권한 확인 > 권한 승인되지 않으면 tmp에 권한 내용 저장
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.CAMERA+" ";
        }

        //카메라 저장 권한 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
        }

        //음성 녹음 권한 확인
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            tmp += Manifest.permission.RECORD_AUDIO+" ";
        }
        //tmp에 내용물이 있다면, 즉 권한 승인받지 못한 권한이 있다면
        if(TextUtils.isEmpty(tmp) == false) {
            //권한 요청하기
            //tts.speak("어플을 이용하기 위해 화면에 뜨는 모든 권한을 허용해 주세요.");
            ActivityCompat.requestPermissions(this, tmp.trim().split(" "), 1);
            return false;
        }else{
            //허용 되어 있으면 그냥 두기
            Log.d("상황: ", "권한 모두 허용");
            return true;
        }

    }
    //권한에 대한 응답이 있을때 자동 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //권한 허용했을 경우
        if(requestCode == 1){
            int length = permissions.length;
            for(int i=0; i<length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
                    //동의
                    Log.d("상황: ","권한 허용 "+permissions[i]);
                }
                else{
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            //Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    //카메라 화면이 업데이트 될 때 실행되는 메소드
    private void updatePreview() {
        Log.i("target","Update");
        if(cameraDevice == null) //카메라를 연결하고 업데이트 메소드를 호출했는데, 만약 카메라 장치가 null값이면 에러 출력
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        //다시 빌더 셋팅
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            //자바의 검증 기능 assert문. 참, 거짓을 검증한다.
            assert  texture != null;
            //이미지 크기 설정
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            //카메라 입장에서 사진촬영 위와 동일함.
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();//화면 업데이트 메소드 호출
                    if(permission_complete == false){
                        permission_complete = true;
                        timer.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SetupCommand();
                            }
                        },1000);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    //카메라 상태 콜백
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        //카메라 장치가 잘 열렸을 때 실행되는 메소드 > 카메라 장치를 TextureView에 연결해서 사용자 화면에 보이게 하기.
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            cameraDevice = camera; //카메라 장치 설정
            createCameraPreview(); //카메라 화면이 보이도록 설정하는 메소드 호출
        }

        //카메라 장치가 연결이 안 됐을 때 실행되는 메소드 > 카메라 장치를 닫는다.
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }

        //카메라 장치에 오류가 났을 때 실행되는 메소드 > 카메라 장치를 닫는다. 장치를 비운다.
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            mCameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            finish();
        }
    };
    //카메라 장치를 여는 것 설정하는 메소드
    private void openCamera(int width, int height) {
        if(permissionCheck() == false) return;
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.CAMERA
            },REQUEST_CAMERA_PERMISSION);
            return;
        }
        setUpCameraOutputs(width, height);
        //카메라 관리하는 매니저 객체
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];  //모든 카메라 종류 중에서 가장 기본 카메라인 0번째 카메라 설정
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId); //0번째 카메라 특성 변수
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //카메라 권한 관련
            //Check realtime permission if run higher API 23
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                manager.openCamera(cameraId,stateCallback,mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        //텍스처뷰 이용 가능할 때 실행되는 메소드 > 카메라 여는 것 설정하는 openCamera 메소드 호출
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 270) % 360;

        return jpegOrientation;
    }
    private void setUpCameraOutputs(int width, int height) {

        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                Point displaySize = new Point();
                ((WindowManager)getBaseContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);

                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                            previewSize.getWidth(), previewSize.getHeight());
                } else {
                    textureView.setAspectRatio(
                            previewSize.getHeight(), previewSize.getWidth());
                }
                String k = String.valueOf(previewSize.getWidth()) + ", " + String.valueOf(previewSize.getHeight());
                Log.i( "target", k);
                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {

        }
    }
    private void takePicture() {
        //장치가 비어있으면 사진을 찍을 수 없으므로 return
        if (cameraDevice == null) return;
        //장치 잘 있으면 카메라 서비스 연결
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            //많은 카메라 중 현재 연결된 camera의 특징을 받아온다.
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());


            //일단 사진 크기는 null값
            Size[] jpegSizes = null;
            //특징 값이 있다면
            if (characteristics != null) {
                //카메라 특징에 맞게 사진 크기 설정
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            //캡처 이미지 사이즈 설정
            int width = textureView.getWidth();
            int height = textureView.getHeight();
            //int width = 640;
            //int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width,height, ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            //캡처 빌더 설정 > 사진 컨트롤, 초점 설정하는 것임.
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //기본 장치 확인
            rotation = getWindowManager().getDefaultDisplay().getRotation();
            rotation = ORIENTATIONS.get(rotation);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,getJpegOrientation(characteristics, rotation));

            //이미지 읽어들이는 리스너
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
                        image.getFormat();
                        buffer = image.getPlanes()[0].getBuffer();
                        bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        bitmap = byteArrayToBitmap(bytes);
                        Global_variable.img = rotatingImageView(rotation,bitmap);
                        ImageUploadToServer();
                    }finally {
                        if (image != null) image.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);

            //사진 촬영 콜백
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            //카메라 입장에서 사진 촬영
            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                //설정
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                //설정 실패
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    //이미지 회전에 쓰이는 메소드
    public Bitmap rotatingImageView(int angle , Bitmap bitmap) {
        //Rotate the image Action
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // Create a new image
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public Bitmap byteArrayToBitmap( byte[] $byteArray ) {
        Bitmap bitmap = BitmapFactory.decodeByteArray( $byteArray, 0, $byteArray.length ) ;
        return bitmap ;
    }

    //뒤에서 다 멈추게 설정
    private void stopBackgoundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //뒤에서 실행되게 하는 메소드
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != cameraCaptureSessions) {
                cameraCaptureSessions.close();
                cameraCaptureSessions = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
    // 서버 관련 함수

    public void ImageUploadToServer(){
        class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

            @Override
            protected String doInBackground(Void... params) { // BackGround에서 동작하는 부분.

                String res = null;
                switch (Global_variable.choice) {
                    case 0: // OCR
                        Global_variable.set_imgString();
                        res = ocr.getOcrRes();
                        break;
                    case 1:  // ImageCaption
                        res = imgCaptioning.getCaption();
                        break;
                    case 2: // VQA
                        Global_variable.set_imgString();
                        try {
                            Global_variable.set_Json();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        res = vqa.getAns();
                    default:
                        break;
                }
                return res;
            }

            @Override
            protected void onPreExecute() { // BackGround 작업이 시작되기 전에 맨처음에 작동하는 부분.

                super.onPreExecute();
                progressDialog = ProgressDialog.show(MainActivity.this, "Image is Uploading", "Please Wait", false, false);
                // Showing progress dialog at image upload time.
            }

            @Override
            protected void onPostExecute(String string1) {// 맨 마지막에 한번만 실행되는 부분.
                /*
                switch (Global_variable.choice){
                    case 0: // OCR
                        Ocr_Page.setVisibility(View.VISIBLE);
                        break;
                    case 1:  // ImageCaption
                        Image_Captioning_Page.setVisibility(View.VISIBLE);
                        break;
                    case 2: // VQA
                        VQA_Page.setVisibility(View.VISIBLE);
                    default:
                        break;
                }*/
                catch_ttsEnd();
                super.onPostExecute(string1);
                progressDialog.dismiss();
                //Success_upload = true;
                Log.i("target",string1);
                if(string1 != "Fail To Connect"){
                    Global_variable.ttxString = string1;
                    tts.speak(Global_variable.ttxString);
                }
                else{
                    tts.speak("서버와의 연결에 실패했습니다.");
                }
            }
        }
        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();

        AsyncTaskUploadClassOBJ.execute();
    }


}