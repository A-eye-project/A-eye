package com.example.A_eye_demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.A_eye_demo.Camera.AutoFitTextureView;
import com.example.A_eye_demo.support.Data_storage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class CameraActivity extends AppCompatActivity {

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
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private String cameraId; //카메라 아이디- 어떤 카메라를 쓸 것인가?
    private CameraDevice cameraDevice; //카메라 장치
    private CameraCaptureSession cameraCaptureSessions; //사진 찍을 때 사용할 변수
    private CaptureRequest.Builder captureRequestBuilder;   //사진 찍기 요청 빌더 변수
    private Size imageDimension; //이미지 치수를 받아오고 전달하는 변수
    private ImageReader imageReader; //이미지 저장할 때 사용하는 변수

    //파일 저장 관련 변수들
    private Handler mcapture; //캡쳐 지연시간 변수

    //비트맵 관련 변수들
    private Bitmap bitmap;
    private int rotation;
    private byte[] bytes;
    private ByteBuffer buffer;

    private RelativeLayout Rl;
    private AutoFitTextureView textureView;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Size previewSize;
    /*
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
*/
    //카메라 화면이 업데이트 될 때 실행되는 메소드
    private void updatePreview() {
        Log.i("target","Update");
        if (cameraDevice == null) //카메라를 연결하고 업데이트 메소드를 호출했는데, 만약 카메라 장치가 null값이면 에러 출력
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        //다시 빌더 셋팅
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try {
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
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();//화면 업데이트 메소드 호출
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Changed", Toast.LENGTH_SHORT).show();
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
            cameraDevice = camera; //카메라 장치 설정
            createCameraPreview(); //카메라 화면이 보이도록 설정하는 메소드 호출
        }

        //카메라 장치가 연결이 안 됐을 때 실행되는 메소드 > 카메라 장치를 닫는다.
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        //카메라 장치에 오류가 났을 때 실행되는 메소드 > 카메라 장치를 닫는다. 장치를 비운다.
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    //카메라 장치를 여는 것 설정하는 메소드
    private void openCamera() {
        //카메라 관리하는 매니저 객체
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];  //모든 카메라 종류 중에서 가장 기본 카메라인 0번째 카메라 설정
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId); //0번째 카메라 특성 변수
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //카메라 권한 관련
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        //텍스처뷰 이용 가능할 때 실행되는 메소드 > 카메라 여는 것 설정하는 openCamera 메소드 호출
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        textureView = (AutoFitTextureView)findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);
        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        //assert textureView != null;
        //textureView.setSurfaceTextureListener(textureListener);
    }
    @Override
    protected void onStart(){
        super.onStart();

        mcapture = new Handler();
        mcapture.postDelayed(new Runnable() {
            @Override
            public void run() {
                takePicture();
            }
        },3000); //

    }
    protected void onPause() {
        super.onPause();
        stopBackgoundThread(); //이건 CameraActivity에서만 쓰는 메소드
    }
    protected void onStop() {
        super.onStop();
        cameraDevice.close();
    }
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        //텍스쳐뷰가 이용가능하면 카메라 여는 것 설정하는 openCamera 메소드 호출
        if (textureView.isAvailable()) openCamera();
        else textureView.setSurfaceTextureListener(textureListener);
    }
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
                        Data_storage.img = rotatingImageView(rotation,bitmap);
                        Data_storage.Flag = true;


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
                    finish();
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

    //카메라 권한 결과
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            //권한 허가 안 받았으면
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
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
            mcapture = null;
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

    public Bitmap Rotate_bitmap(Bitmap myimg){
        Matrix matrix = new Matrix();
        //이미지의 각도를 90 주는 이유
        //Preview 에서는 1280 * 720(반대로 720*1280 지원하지 않음.)을 지원하기 때문에,
        //해당 이미지의 경우 90를 회전 시켜준다.
        matrix.postRotate(90);

        //새롭게 만들어진 bitmap으로 이미지 파일을 생성한다.
        Bitmap new_bit =  Bitmap.createBitmap(myimg, 0, 0, myimg.getWidth(), myimg.getHeight(), matrix, true);
        return new_bit;
    }
}

/*
밑에 기본적인 Camera2basic에 관한 설명

+ 버튼을 없애고 Handler를 사용해 2초가 지난후 사진이 찍히도록 만듬.

 */


/*
1. onResume() 호출
2. 메인 Thread를 방해하지 않기 위해 새로운 Thread와 Handler 생성
3. TexturesView 인플레이팅 + 초기화
4. 처음 Acticity 실행시 TextureView 초기화 완료 후 onSurfaceTextureAvailable()호출

5. onSurfaceTextureAvailable--
 -1) textureView 사이즈 입력받고 조정
 -2) Camera Runtime 권한 획득 여부 확인후 setUpCameraOutput실행

6. 후면 카메라 선택
 -1)캡쳐된 사진(이미지리더)의 해상도, 포맷 선택 <- 이미지 변경하려면 이 부분에서 수행하면 될 듯 .
 -2)이미지의 방향
 -3)적합한 프리뷰 사이즈 선택
 -4)들어오는 영상의 비율에 맞춰 TextureView의 비율 변경(AutoFitTextureView 커스텀 뷰)
 -5)플래시 지원 여부 확인

7. configureTransform()
스크린과 카메라의 영상의 방향을 맞추기 위해 View를 매트릭스 연산으로 회전

8. 카메라를 열기위한 선 작업 후
CameraManager를 통해 openCamera(카메라ID, CameraDevice.StateCallback, 핸들러) 호출

9. 카메라가 열리고 onOpend(cameraDevice) 호출, 정상적으로 진행됬다면 프리뷰세션 만듬. -> createCameraPreviewSession 실행

10. createCameraPreviewSession
 -1). SurfaceTexture에 setUpCameraOutputs()에서 계산한 기본 버퍼 사이즈를 설정 후 surface 생성
 -2). CaptureRequest.Builder의 Target으로 surface(View)를 선택.
 -3). Target은 실제 카메라 프레임 버퍼를 받아 처리하기 때문에 먼저, 캡쳐세션을 만들어 나중에 CaputureRequest를 사용할 수 있게 함.
 -4). CameraDevice.createCaptureSession()을 통해 세션생성
 -5). 세션 생성 후 CameraCaptureSession.StateCallback의 onConfigured() 호출
 -6). onConfigured()에서 CaptureRequest.Builder인스턴스를 build()하여 CaptureRequest객체
 -7). 반복적으로 이미지 버퍼를 얻기 위해 프리뷰 세션에서 setRepeatingRequest()를 호출 ->  TextureView에 카메라 영상이 나오는것을 확인

11. Capture

카메라에게 초점을 잡으라고 request를 보내기 위해 해당 파라미터를 설정
준비된 세션으로부터 capture()메소드와 함께 request인자를 넣어 호출
초점이 잡혔다면 mCaptureCallback으로부터 captureStillPicture()을 호출


 */
