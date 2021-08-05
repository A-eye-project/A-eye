package com.example.a_eye_demo.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.example.a_eye_demo.Server.Get_string;

public class Recording {
    int maxLenSpeech = 16000 * 45;
    byte [] speechData = new byte [maxLenSpeech * 2];
    int lenSpeech = 0;
    boolean isRecording = false;
    boolean forceStop = false;
    String Result = "";


    public void Start_record(){
        recordSpeech();
    }

    public void Stop_record(){
        forceStop = true;
    }

    public void recordSpeech() throws RuntimeException {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audio = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            lenSpeech = 0;
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("ERROR: Failed to initialize audio device. Allow app to access microphone");
            }
            else {
                short [] inBuffer = new short [bufferSize];
                forceStop = false;
                isRecording = true;
                audio.startRecording();
                while (!forceStop) {
                    int ret = audio.read(inBuffer, 0, bufferSize);
                    for (int i = 0; i < ret ; i++ ) {
                        if (lenSpeech >= maxLenSpeech) {
                            forceStop = true;
                            break;
                        }
                        speechData[lenSpeech*2] = (byte)(inBuffer[i] & 0x00FF);
                        speechData[lenSpeech*2+1] = (byte)((inBuffer[i] & 0xFF00) >> 8);
                        lenSpeech++;
                    }
                }
                audio.stop();
                audio.release();
                isRecording = false;
            }
        } catch(Throwable t) {
            throw new RuntimeException(t.toString());
        }
    }
    public int net_com(){
        Thread threadRecog = new Thread(new Runnable() {// API 통신을 위한 Thread -> 이거 없이 메인에서 함수 부르면 Error 발생
            public void run() {
                Result = Get_string.sendDataAndGetResult(speechData,lenSpeech);
            }
        });
        threadRecog.start();
        try {
            threadRecog.join(20000);
            if (threadRecog.isAlive()) {
                threadRecog.interrupt();
                return -2;// "No response from server for 20 secs";
            }
            else {
                return 1;//"통신완료";
            }
        } catch (InterruptedException e) {
            return -1;// "Interrupted";
        }
    }

    public byte [] get_speechData(){
        return speechData;
    }
    public String get_re(){
        return Result;
    }

    public int get_lenSp(){
        return lenSpeech;
    }
}
