package com.example.a_eye.Camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
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
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.a_eye.Audio.CommandService;
import com.example.a_eye.Audio.Recording;
import com.example.a_eye.Audio.TTSAdapter;
import com.example.a_eye.MainActivity;
import com.example.a_eye.R;
import com.example.a_eye.Server.Image_Captioning;
import com.example.a_eye.Server.OCR;
import com.example.a_eye.Server.VQA;
import com.example.a_eye.Support.Global_variable;
import com.example.a_eye.Support.Select_Function;
import com.example.a_eye.Support.Set_Dialog;

import org.json.JSONException;

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


public class Camera_Fragment extends Fragment
        implements ActivityCompat.OnRequestPermissionsResultCallback {


    /**
     * upload {@link }
     * 맨 마지막 부분
     * Caputre {@Link}
     * 242 번째 줄
     *
     */
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static Context mainContext;
    /*

    *My Variable

     */
    private Size imageDimension; //이미지 치수를 받아오고 전달하는 변수

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Handler timer = new Handler();
    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    //카메라 상태 콜백
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        //카메라 장치가 잘 열렸을 때 실행되는 메소드 > 카메라 장치를 TextureView에 연결해서 사용자 화면에 보이게 하기.
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        //카메라 장치가 연결이 안 됐을 때 실행되는 메소드 > 카메라 장치를 닫는다.
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        //카메라 장치에 오류가 났을 때 실행되는 메소드 > 카메라 장치를 닫는다. 장치를 비운다.
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;


    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Bitmap bitmap;
            byte[] bytes;
            ByteBuffer buffer;
            Image image = null;
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            rotation = ORIENTATIONS.get(rotation);
            try{
                image = reader.acquireLatestImage();
                image.getFormat();
                buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                bitmap = byteArrayToBitmap(bytes);
                bitmap = rotatingImageView(rotation,bitmap);
                Global_variable.resized = Bitmap.createScaledBitmap(bitmap, 480, 640, true);
                ImageUploadToServer();
            }finally {
                if (image != null) image.close();
            }
        }

    };

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

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    /*
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }


        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };
     */

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */

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
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera_Fragment newInstance(Context main) {
        mainContext = main;
        return new Camera_Fragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //LayoutInflater은 XML에 미리 정의해둔 틀을 실제 메모리에 올려주는 역할을 한다.
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_fragment, container, false);

        //LayoutInflater의 inflate() 메서드로 Layout을 inflate 한 경우에는 폴더별(Land, Port) Layout을 저절로 참조 하게 된다.
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture); // 화면 그자체 com.example.android.camera2basic.AutoFitTextureView
        //command = new Command(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        Log.i("here2","onStop");
        //closeAudio();
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    public boolean permissionCheck(){
        //권한이 허용되어 있는지 확인한다.
        String tmp = "";

        //카메라 권한 확인 > 권한 승인되지 않으면 tmp에 권한 내용 저장
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.CAMERA+" ";
        }

        //카메라 저장 권한 확인
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
        }

        //음성 녹음 권한 확인
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.RECORD_AUDIO+" ";
        }

        //tmp에 내용물이 있다면, 즉 권한 승인받지 못한 권한이 있다면
        if (TextUtils.isEmpty(tmp) == false) {
            //권한 요청하기
            //tts.speak("어플을 이용하기 위해 화면에 뜨는 모든 권한을 허용해 주세요.");
            ActivityCompat.requestPermissions(getActivity(), tmp.trim().split(" "), 1);
            return false;
        } else {
            //허용 되어 있으면 그냥 두기
            Log.d("상황: ", "권한 모두 허용");
            return true;
        }

    }

    //권한에 대한 응답이 있을때 자동 작동하는 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //권한 허용했을 경우
        if (requestCode == 1){
            int length = permissions.length;
            for (int i = 0; i < length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    //동의
                    Log.d("상황: ","권한 허용 "+permissions[i]);
                } else {
                    getActivity().finish();
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */

    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
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

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                //Size mysize = new Size(maxPreviewWidth,maxPreviewHeight);
                //mPreviewSize = mysize;
                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link Camera_Fragment#mCameraId}.
     */

    //카메라 장치를 여는 것 설정하는 메소드
    private void openCamera(int width, int height) {
        if(permission_complete == false) if(permissionCheck() == false) return;
        if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.CAMERA
            },REQUEST_CAMERA_PERMISSION);
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();

        //카메라 관리하는 매니저 객체
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = manager.getCameraIdList()[0];  //모든 카메라 종류 중에서 가장 기본 카메라인 0번째 카메라 설정
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId); //0번째 카메라 특성 변수
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //카메라 권한 관련
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */

    //뒤에서 실행되게 하는 메소드
    private void startBackgroundThread() {  //Main Thread == UI Thread 는 남겨놓고 Background 형태로 Camera를 계속해서 동작시킨다.(켜 놓는다?)
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     *
     * 안드로이드 화면을 구성하는 뷰나 뷰그룹을 하나의 스레드에서만 담당하는 원칙을 싱글 스레드 모델이라고 합니다.
     * 싱글 스레드 모델의 규칙은 첫째, 메인 스레드(UI 스레드)를 블럭하지 말 것,
     * 둘째, 안드로이드 UI 툴킷은 오직 UI 스레드에서만 접근할 수 있도록 할 것, 이 두 가지입니다.
     *  긴 시간이 걸리는 작업을 메인 스레드에서 담당한다면 애플리케이션의 반응성이 낮아질 수 있고,
     *  급기야 사용자의 불편함을 방지하고자 시스템이 애플리케이션을 ANR(Appication Not Responding) 상태로 전환시킬 수도 있습니다.
     *  따라서 시간이 걸리는 작업을 하는 코드는 여분의 스레드를 사용하여 메인 스레드에서 분리해야 하고, 자연스럽게 메인 스레드와 다른 스레드가 통신하는 방법이 필요하게 됩니다.
     * Stops the background thread and its {@link Handler}.
     */

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely(); // 카메라 중단
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */

    //카메라 화면이 업데이트 될 때 실행되는 메소드
    private void updatePreview() {
        Log.i("target","Update");
        if(mCameraDevice == null) //카메라를 연결하고 업데이트 메소드를 호출했는데, 만약 카메라 장치가 null값이면 에러 출력
            Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
        //다시 빌더 셋팅
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean permission_complete = false;
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            //이미지 크기 설정
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            //카메라 입장에서 사진촬영 위와 동일함.
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            updatePreview();//화면 업데이트 메소드 호출

                            // Catch Start Recording
                            if (permission_complete == false) {
                                permission_complete = true;
                                catch_start_recording();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.i("error","Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        //장치가 비어있으면 사진을 찍을 수 없으므로 return
        if (mCameraDevice == null) return;

        //장치 잘 있으면 카메라 서비스 연결
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try{
            //많은 카메라 중 현재 연결된 camera의 특징을 받아온다.
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());

            //일단 사진 크기는 null값
            Size[] jpegSizes = null;

            //특징 값이 있다면
            if (characteristics != null) {
                //카메라 특징에 맞게 사진 크기 설정
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            //캡처 이미지 사이즈 설정
            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();
            //int width = 640;
            //int height = 480;

            if(jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width,height, ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));

            //캡처 빌더 설정 > 사진 컨트롤, 초점 설정하는 것임.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //기본 장치 확인
            int rotation1 = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            final int rotation = ORIENTATIONS.get(rotation1);

            //이미지 읽어들이는 리스너
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    Bitmap bitmap;
                    byte[] bytes;
                    ByteBuffer buffer;

                    try {
                        image = reader.acquireLatestImage();
                        image.getFormat();
                        buffer = image.getPlanes()[0].getBuffer();
                        bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        bitmap = byteArrayToBitmap(bytes);
                        bitmap = rotatingImageView(rotation,bitmap);

                        // VQA랑 ImageCaption 만  이미지 조절 해야할거 같음. (확인)
                        Global_variable.resized = Bitmap.createScaledBitmap(bitmap, 480, 640, true);
                        ImageUploadToServer();
                    } finally {
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
                    createCameraPreviewSession();
                }
            };

            //카메라 입장에서 사진 촬영
            mCameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
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

    /*
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    /*
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }*/


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }


    //각종 변수
    private boolean isstart = false;
    private AsyncTaskUploadClass AsyncTaskUploadClassOBJ;
    Image_Captioning imgCaptioning= new Image_Captioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();
    Set_Dialog setDialog = new Set_Dialog(mainContext);
    TTSAdapter myspeaker = new TTSAdapter(mainContext);

    private void catch_start_recording() { //
        Timer Catch = new Timer();
        TimerTask start_recording = new TimerTask() {
            @Override
            public void run() {
                if(CommandService.command.startFunction == true){
                    get_record_string();
                    CommandService.command.startFunction = false;
                    Catch.cancel();
                }

            }
        };

        Catch.schedule(start_recording,1000,300);
    }

    // TTS 가 끝나면
    private void catch_ttsEnd() { // tts 끝날 시 종료
        Timer TTS = new Timer();
        TimerTask tts_end = new TimerTask() {
            @Override
            public void run() {
                if(myspeaker.tts.isSpeaking() == false) {
                    CommandService.command.StartListening();
                    TTS.cancel();
                }
            }
        };
        TTS.schedule(tts_end,3000,1000);
    }


    // Core Function
    private void get_record_string(){
        if(MainActivity.activity_die == true) return;
        Recording myRecord = new Recording();
        new Thread(new Runnable() { //새 Thread에서 녹음 시작
            public void run() {
                try {
                    Log.i("cur","Recording.. ");
                    myRecord.Start_record();
                } catch (RuntimeException e) {
                    Log.i("Error", e.getMessage());
                    return;
                }
            }
        }).start();

        timer.postDelayed(new Runnable(){ // 녹음 하는 시간 지연
            @Override
            public void run(){
                myRecord.Stop_record(); // 녹음 종료
                Log.i("cur","Reconizing.. ");
                int status = myRecord.net_com(); // 녹음 파일 -> String으로 바꾸는 API 통신 , return값은 통신 상태
                if(status == 1){
                    String Result = myRecord.get_re();
                    Log.i("cur",Result);
                    get_num(Result);
                } else {
                    if (status == -2) {
                        Log.i("cur","No response from server for 20 secs");
                    } else {
                        Log.i("cur","Interrupted");
                    }
                }
            }
        },3000); // 녹음 시간 -> 현재 3초


    }

    private void get_num(String Result){
        Select_Function my = new Select_Function();
        my.Set_str(" "+ Result.replaceAll(" ",""));
        my.Local_Alignment();

        // 음성인식 종료
        if ((Global_variable.question.contains("꺼줘") == true)||(Global_variable.question.contains("꺼져") == true)||(Global_variable.question.contains("꺼저") == true)||(Global_variable.question.contains("꿔저") == true)){
            getActivity().stopService(MainActivity.serviceIntent);
            getActivity().finish();
        } else {
            takePicture();
        }
    }

    private void ImageUploadToServer(){
        isstart = true;
        AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();
        AsyncTaskUploadClassOBJ.execute();
    }

    class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) { // BackGround에서 동작하는 부분.

            String res = null;
            switch (Global_variable.choice) {
                case 0: // OCR
                    Log.i("cur","OCR");
                    Global_variable.set_imgString(false);
                    res = ocr.getOcrRes();
                    break;
                case 1:  // ImageCaption
                    Log.i("cur","ImageCaption");
                    res = imgCaptioning.getCaption();
                    break;
                case 2: // VQA
                    Log.i("cur","VQA");
                    Global_variable.set_imgString(true);
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
            setDialog.setup();
            setDialog.show();
        }

        @Override
        protected void onPostExecute(String string1) {// 맨 마지막에 한번만 실행되는 부분
            catch_ttsEnd();
            super.onPostExecute(string1);
            setDialog.dismiss();
            Log.i("target",string1);
            if(string1 != "Fail To Connect"){
                Global_variable.ttxString = string1;
                myspeaker.speak(Global_variable.ttxString);
            }
            else{
                myspeaker.speak("서버와의 연결에 실패했습니다.");
            }
        }
    }
}