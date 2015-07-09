package com.honsal.geosource.recorders;

import android.media.AudioRecord;
import android.os.*;

import com.honsal.geosource.encoders.AudioEncoder;

/**
 * Created by 이 on 2015-07-10.
 */
public class MicRecorder implements Runnable {

    private AudioEncoder encoder;
    private AudioRecord recorder;
    private final int SAMPLE_RATE;
    private final int CHANNEL_COUNT;
    private final int CHANNEL_BITS;
    private final int BUFFER_125MS_SIZE;
    private Thread thread;
    private boolean shouldStop = false;

    public MicRecorder(AudioEncoder encoder, int samplerate, int channelcount, int channelbits, int buffer125mssize) {
        this.encoder = encoder;
        SAMPLE_RATE = samplerate;
        CHANNEL_COUNT = channelcount;
        CHANNEL_BITS = channelbits;
        BUFFER_125MS_SIZE = buffer125mssize;

        recorder = encoder.getRecorder();
        thread = new Thread(this, "MicRecordingThread");
    }

    public void start() {
        thread.start();
        shouldStop = false;
    }

    public void stop() {
        shouldStop = true;
    }

    private void stopRecorder() {
        recorder.stop();
        recorder.release();
    }

    public boolean isRecording() {
        return !shouldStop;
    }

    private void record() {
        byte[] data = new byte[BUFFER_125MS_SIZE];

        recorder.read(data, 0, BUFFER_125MS_SIZE);
        encoder.pushInputFrame(data);
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        while(!shouldStop) {
            record();
        }
    }
}
