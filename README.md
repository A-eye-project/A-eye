# A-eye

## 📢 소개글 / Introduction 📢
```
저시력자와 시각장애인의 일상생활의 도움을 주기 위한 어플리케이션 A EYE 입니다.
사진촬영을 통해 주변에 있는 사물과 문자를 분석하여 사용자에게 정보를 제공합니다.
```

## 서버 설치 절차 / Installation For Server
 
## 요구 사항 / Requirements
### 📱 어플리케이션 / For Application
```
[접근 권한 안내]
카메라 : 카메라 API 를 통해 카메라를 제어하고 사진 촬영을 합니다.
마이크 : 음성 인식을 위해 사용하며, 어플리케이션이 켜져있는 동안 음성을 인식합니다.
저장공간 : 촬영된 이미지를 임시로 저장하고, 이미지 분석이 끝난 후에 자동으로 삭제됩니다.

[안드로이드 8.0 이상 버전에서만 A EYE 의 정상 이용이 가능합니다.]
```

### ✔ 서버 / For Server
##### 📕 라이브러리 / Library Requirements
```
pytorch 1.7.1
torchvision that matches the pytorch installation
opencv
pyyaml
tqdm
matplotlib
requests
scikit-image
anytree
regex
boto3
h5py
nltk
joblib
pandas
scipy
pycocotools
yacs
flask
numpy
json
base64
```

##### 💻 시스템 사양 / System Requirements
```
Ubuntu 18.04 or 20.04
python 3.7
CUDA 10.X or 11.X
At least 7GB VRAM
GCC or MSVC
```

## 사용법 / How to Use
```
어플리케이션을 켜신 후 저희가 설정한 키워드 '아담' 을 말하신 후.
두 번째 진동을 느끼신 후 명령을 말씀하시면 됩니다.

OCR 의 경우
'읽어' 와 같은 키워드가 명령에 들어갔을 시 동작합니다.

Image Caption 의 경우
'설명', '묘사' 등의 의미를 가진 표현을 사용하실 시 동작합니다.

VQA 의 경우
그 외 궁금하신 질문의 경우 답변을 해드립니다.

또한 어플리케이션은 완전 종료를 하지 않는 이상 백그라운드에서 실행되며 키워드를 통해 동작을 하실 수 있습니다.
```
