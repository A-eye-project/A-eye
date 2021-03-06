package com.example.a_eye.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.example.a_eye.Server.Get_string;

public class Recording {
    int maxLenSpeech = 16000 * 45;
    byte[] speechData = new byte[maxLenSpeech * 2];
    int lenSpeech = 0;
    boolean isRecording = false;
    boolean forceStop = false;
    String Result = "";


    public void Start_record() {
        recordSpeech();
    }

    public void Stop_record() {
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
            } else {
                short[] inBuffer = new short[bufferSize];
                forceStop = false;
                isRecording = true;
                audio.startRecording();
                while (!forceStop) {
                    int ret = audio.read(inBuffer, 0, bufferSize);
                    for (int i = 0; i < ret; i++) {
                        if (lenSpeech >= maxLenSpeech) {
                            forceStop = true;
                            break;
                        }
                        speechData[lenSpeech * 2] = (byte) (inBuffer[i] & 0x00FF);
                        speechData[lenSpeech * 2 + 1] = (byte) ((inBuffer[i] & 0xFF00) >> 8);
                        lenSpeech++;
                    }
                }

                audio.stop();
                audio.release();
                isRecording = false;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t.toString());
        }
    }

    public int net_com() {
        Thread threadRecord = new Thread(new Runnable() { // API ????????? ?????? Thread -> ?????? ?????? ???????????? ?????? ????????? Error ??????
            public void run() {
                Result = Get_string.sendDataAndGetResult(speechData, lenSpeech);
            }
        });

        threadRecord.start();

        try {
            threadRecord.join(20000);
            if (threadRecord.isAlive()) {
                threadRecord.interrupt();
                return -2;// "No response from server for 20 secs";
            } else {
                return 1;//"????????????";
            }
        } catch (InterruptedException e) {
            return -1;// "Interrupted";
        }
    }

    public String get_result() {
        return Result;
    }
}
