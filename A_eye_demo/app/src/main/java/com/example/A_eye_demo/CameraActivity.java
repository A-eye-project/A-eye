package com.example.A_eye_demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.A_eye_demo.Camera.Camera_Fragment;
import com.example.A_eye_demo.R;

public class CameraActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera_Fragment.newInstance())
                    .commit();
        }
    }
}
/*
밑에 기본적인 Camera2basic에 관한 설명

+ 버튼을 없애고 Handler를 사용해 2초가 지난후 사진이 찍히도록 만듬.
사진 저장 후 Activity 종료되도록 OnDestroy() 호출.

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
