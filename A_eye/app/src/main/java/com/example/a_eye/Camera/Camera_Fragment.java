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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

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
     * ??? ????????? ??????
     * Caputre {@Link}
     * 242 ?????? ???
     *
     */
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static Context mainContext;
    /*
    *My Variable
     */
    private Size imageDimension; //????????? ????????? ???????????? ???????????? ??????

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
    //????????? ?????? ??????
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        //????????? ????????? ??? ????????? ??? ???????????? ????????? > ????????? ????????? TextureView??? ???????????? ????????? ????????? ????????? ??????.
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        //????????? ????????? ????????? ??? ?????? ??? ???????????? ????????? > ????????? ????????? ?????????.
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        //????????? ????????? ????????? ?????? ??? ???????????? ????????? > ????????? ????????? ?????????. ????????? ?????????.
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

            try {
                image = reader.acquireLatestImage();
                image.getFormat();
                buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                bitmap = byteArrayToBitmap(bytes);
                bitmap = rotatingImageView(rotation,bitmap);
                Global_variable.resized = Bitmap.createScaledBitmap(bitmap, 480, 640, true);
                ImageUploadToServer();
            } finally {
                if (image != null) image.close();
            }
        }

    };

    //????????? ????????? ????????? ?????????
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //LayoutInflater??? XML??? ?????? ???????????? ?????? ?????? ???????????? ???????????? ????????? ??????.
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_fragment, container, false);

        //LayoutInflater??? inflate() ???????????? Layout??? inflate ??? ???????????? ?????????(Land, Port) Layout??? ????????? ?????? ?????? ??????.
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture); // ?????? ????????? com.example.android.camera2basic.AutoFitTextureView
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // ????????? ?????? ??????
        if(permission_complete == true) {
            catch_start_recording();
        }

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
        closeCamera();
        stopBackgroundThread();

        try {
            Catch.cancel();
            TTS.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onPause();
    }


    public boolean permissionCheck(){
        //????????? ???????????? ????????? ????????????.
        String tmp = "";

        //????????? ?????? ?????? > ?????? ???????????? ????????? tmp??? ?????? ?????? ??????
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.CAMERA+" ";
        }

        //????????? ?????? ?????? ??????
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.WRITE_EXTERNAL_STORAGE+" ";
        }

        //?????? ?????? ?????? ??????
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
            tmp += Manifest.permission.RECORD_AUDIO+" ";
        }

        //tmp??? ???????????? ?????????, ??? ?????? ???????????? ?????? ????????? ?????????
        if (TextUtils.isEmpty(tmp) == false) {
            //?????? ????????????
            //tts.speak("????????? ???????????? ?????? ????????? ?????? ?????? ????????? ????????? ?????????.");
            ActivityCompat.requestPermissions(getActivity(), tmp.trim().split(" "), 1);
            return false;
        } else {
            //?????? ?????? ????????? ?????? ??????
            Log.d("??????: ", "?????? ?????? ??????");
            return true;
        }

    }

    //????????? ?????? ????????? ????????? ?????? ???????????? ??????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //?????? ???????????? ??????
        if (requestCode == 1) {
            int length = permissions.length;
            for (int i = 0; i < length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    //??????
                    Log.d("??????: ","?????? ?????? "+permissions[i]);
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

    //????????? ????????? ?????? ??? ???????????? ?????????
    private void openCamera(int width, int height) {
        if (permission_complete == false) {
            if(permissionCheck() == false) return;
        }

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.CAMERA
            },REQUEST_CAMERA_PERMISSION);
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();

        //????????? ???????????? ????????? ??????
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = manager.getCameraIdList()[0];  //?????? ????????? ?????? ????????? ?????? ?????? ???????????? 0?????? ????????? ??????
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId); //0?????? ????????? ?????? ??????
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //????????? ?????? ??????
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
        Log.d("?????????", "????????? ??????");
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

    //????????? ???????????? ?????? ?????????
    private void startBackgroundThread() {  //Main Thread == UI Thread ??? ???????????? Background ????????? Camera??? ???????????? ???????????????.(??? ??????????)
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     *
     * ??????????????? ????????? ???????????? ?????? ???????????? ????????? ?????????????????? ???????????? ????????? ?????? ????????? ??????????????? ?????????.
     * ?????? ????????? ????????? ????????? ??????, ?????? ?????????(UI ?????????)??? ???????????? ??? ???,
     * ??????, ??????????????? UI ????????? ?????? UI ?????????????????? ????????? ??? ????????? ??? ???, ??? ??? ???????????????.
     *  ??? ????????? ????????? ????????? ?????? ??????????????? ??????????????? ????????????????????? ???????????? ????????? ??? ??????,
     *  ????????? ???????????? ???????????? ??????????????? ???????????? ????????????????????? ANR(Appication Not Responding) ????????? ???????????? ?????? ????????????.
     *  ????????? ????????? ????????? ????????? ?????? ????????? ????????? ???????????? ???????????? ?????? ??????????????? ???????????? ??????, ??????????????? ?????? ???????????? ?????? ???????????? ???????????? ????????? ???????????? ?????????.
     * Stops the background thread and its {@link Handler}.
     */

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely(); // ????????? ??????
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

    //????????? ????????? ???????????? ??? ??? ???????????? ?????????
    private void updatePreview() {
        Log.i("target","Update");
        if (mCameraDevice == null) //???????????? ???????????? ???????????? ???????????? ???????????????, ?????? ????????? ????????? null????????? ?????? ??????
            Log.i("????????? ????????????","NULL");

        //?????? ?????? ??????
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static boolean permission_complete = false;
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            //????????? ?????? ??????
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            //????????? ???????????? ???????????? ?????? ?????????.
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            updatePreview();//?????? ???????????? ????????? ??????

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
        //????????? ??????????????? ????????? ?????? ??? ???????????? return
        if (mCameraDevice == null) return;

        //?????? ??? ????????? ????????? ????????? ??????
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try{
            //?????? ????????? ??? ?????? ????????? camera ??? ????????? ????????????.
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());

            //?????? ?????? ????????? null ???
            Size[] jpegSizes = null;

            //?????? ?????? ?????????
            if (characteristics != null) {
                //????????? ????????? ?????? ?????? ?????? ??????
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            //?????? ????????? ????????? ??????
            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();

            if(jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width,height, ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));

            //?????? ?????? ?????? > ?????? ?????????, ?????? ???????????? ??????.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //?????? ?????? ??????
            int rotation1 = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            final int rotation = ORIENTATIONS.get(rotation1);

            //????????? ??????????????? ?????????
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

                        Global_variable.img = bitmap;

                        // VQA??? ImageCaption ????????? ??????
                        Global_variable.resized = Bitmap.createScaledBitmap(bitmap, 480, 640, true);
                        ImageUploadToServer();
                    } finally {
                        if (image != null) image.close();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);

            //?????? ?????? ??????
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreviewSession();
                }
            };

            //????????? ???????????? ?????? ??????
            mCameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                //??????
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                //?????? ??????
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

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


    //?????? ??????
    private AsyncTaskUploadClass AsyncTaskUploadClassOBJ;
    private Vibrator vibrator = (Vibrator)mainContext.getSystemService(mainContext.VIBRATOR_SERVICE);
    Image_Captioning imgCaptioning= new Image_Captioning();
    OCR ocr = new OCR();
    VQA vqa = new VQA();
    Set_Dialog setDialog = new Set_Dialog(mainContext);
    TTSAdapter myspeaker = new TTSAdapter(mainContext);

    Timer Catch;
    Timer TTS;

    private void catch_start_recording() {
        Catch = new Timer();
        TimerTask start_recording = new TimerTask() {
            @Override
            public void run() {
                if (CommandService.command.startFunction == true){
                    get_record_string();
                    CommandService.command.startFunction = false;
                    Catch.cancel();
                }
            }
        };

        Catch.schedule(start_recording,1000,300);
    }

    // TTS ??? ?????????
    private void catch_ttsEnd() { // tts ?????? ??? ??????
        TTS = new Timer();
        TimerTask tts_end = new TimerTask() {
            @Override
            public void run() {
                if (myspeaker.tts.isSpeaking() == false) {
                    setDialog.result_dismiss();
                    CommandService.start_listening();
                    catch_start_recording();
                    TTS.cancel();

                    if (Global_variable.call_service) {
                        Global_variable.call_service = false;
                        getActivity().finish();
                    }
                }
            }
        };

        TTS.schedule(tts_end,3000,1000);
    }


    // Core Function
    private void get_record_string(){
        if (MainActivity.activity_die == true) return;
        Recording myRecord = new Recording();

        new Thread(new Runnable() { //??? Thread?????? ?????? ??????
            public void run() {
                try {
                    command_vive();
                    Log.i("cur","Recording.. ");
                    myRecord.Start_record();
                } catch (RuntimeException e) {
                    Log.i("Error", e.getMessage());
                    return;
                }
            }
        }).start();

        timer.postDelayed(new Runnable(){ // ?????? ?????? ?????? ??????
            @Override
            public void run(){
                myRecord.Stop_record(); // ?????? ??????
                Log.i("cur","Reconizing.. ");
                int status = myRecord.net_com(); // ?????? ?????? -> String?????? ????????? API ?????? , return?????? ?????? ??????
                if(status == 1){
                    String Result = myRecord.get_result();
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
        },4000); // ?????? ?????? -> ?????? 3???
    }

    private void get_num(String Result){
        Select_Function my = new Select_Function();
        my.Set_str(" "+ Result.replaceAll(" ",""));
        my.Local_Alignment();

        // ???????????? ??????
        if ((Global_variable.question.contains("??????") == true) || (Global_variable.question.contains("??????") == true)
                || (Global_variable.question.contains("??????") == true) || (Global_variable.question.contains("??????") == true)) {
            getActivity().stopService(MainActivity.serviceIntent);
            getActivity().finish(); //??????????????? ??????
        } else {
            takePicture();
        }
    }

    private void ImageUploadToServer(){
        AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();
        AsyncTaskUploadClassOBJ.execute();
    }

    class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) { // BackGround?????? ???????????? ??????.

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
        protected void onPreExecute() { // BackGround ????????? ???????????? ?????? ???????????? ???????????? ??????.
            super.onPreExecute();
            setDialog.loding_setup();
            setDialog.loading_show();
        }

        @Override
        protected void onPostExecute(String string1) {// ??? ???????????? ????????? ???????????? ??????
            super.onPostExecute(string1);
            catch_ttsEnd();
            setDialog.result_setup();
            setDialog.loading_dismiss();
            Log.i("target",string1);

            if (string1 != "Fail To Connect") {
                Global_variable.ttxString = string1;
                setDialog.result_str(Global_variable.question, Global_variable.ttxString);
                myspeaker.speak(Global_variable.ttxString);
            } else {
                setDialog.result_str("", "???????????? ????????? ??????????????????.");
                myspeaker.speak("???????????? ????????? ??????????????????.");
            }

            setDialog.result_show();
        }
    }

    private void command_vive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200,20));
        } else {
            vibrator.vibrate(200);
        }
    }
}