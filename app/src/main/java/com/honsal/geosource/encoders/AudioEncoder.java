package com.honsal.geosource.encoders;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.honsal.geosource.etc.ToastOnUIThread;
import com.honsal.geosource.recorders.MicRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by 이 on 2015-07-08.
 */
public class AudioEncoder implements Runnable {
    private static final String TAG = "AudioEncoder";
    private final int BIT_RATE;
    private final String MIME;
    private final int CHANNEL_COUNT;
    private final int SAMPLE_RATE;
    private final int AAC_PROFILE;
    private final int SBR_MODE;
    private final int AUDIO_FORMAT;
    private final Context CONTEXT;
    private AudioRecord recorder;
    private int channelcfg;
    private int BUFFER_SIZE;
    private final int SAMPLES_PER_SEC;
    private final int BUFFER_500MS_SIZE;
    private MediaCodec encoder;
    private MicRecorder micRecorder;
    private Thread thread;
    private Object lock = new Object();
    private boolean shouldClose = false;
    private ArrayBlockingQueue<byte[]> input;
    private int inputSize = 100;
    private ArrayBlockingQueue<byte[]> output;
    private int outputSize = 100;
    private VideoEncoder videoEncoder;

    private boolean shouldEncode = false;


    public AudioEncoder(int bitrate, String mime, int channelcount, int samplerate, int aacprofile, int sbrmode, int audioformat, Context context) {
        BIT_RATE = bitrate;
        MIME = mime;
        CHANNEL_COUNT = channelcount;
        SAMPLE_RATE = samplerate;
        AAC_PROFILE = aacprofile;
        SBR_MODE = sbrmode;
        CONTEXT = context;
        AUDIO_FORMAT = audioformat;

        switch (CHANNEL_COUNT) {
            case 1:
                channelcfg = AudioFormat.CHANNEL_IN_MONO;
            case 2:
                channelcfg = AudioFormat.CHANNEL_IN_STEREO;
            default:
                channelcfg = AudioFormat.CHANNEL_IN_MONO;
        }
        
        BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelcfg, AUDIO_FORMAT);

        SAMPLES_PER_SEC = SAMPLE_RATE / CHANNEL_COUNT / (AUDIO_FORMAT == AudioFormat.ENCODING_PCM_16BIT ? 2 : 1);

        BUFFER_500MS_SIZE = SAMPLES_PER_SEC / 2;

        thread = new Thread(this, "AudioEncodingThread");
    }

    private void setupRecorder() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelcfg, AUDIO_FORMAT, BUFFER_SIZE * 10);
        micRecorder = new MicRecorder(this, SAMPLE_RATE, CHANNEL_COUNT, (AUDIO_FORMAT == AudioFormat.ENCODING_PCM_16BIT ? 2 : 1), BUFFER_500MS_SIZE);
    }

    public void setVideoEncoder(VideoEncoder videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    private void setupEncoder() {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNEL_COUNT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AAC_PROFILE);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_SBR_MODE, SBR_MODE);

        try {
            encoder = MediaCodec.createEncoderByType(MIME);
        } catch (IOException e) {
            Log.e(TAG, "An error occured while creating encoder: " + e.toString());
            ToastOnUIThread.makeTextLongExit(CONTEXT, "An error occured while creating encoder: " + e.toString());
        }

        try {
            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot use incompatible codec: " + e.toString());
            ToastOnUIThread.makeTextLongExit(CONTEXT, "Cannot use incompatible codec: " + e.toString());
        }

        input = new ArrayBlockingQueue<byte[]>(inputSize);
        output = new ArrayBlockingQueue<byte[]>(outputSize);
    }

    public AudioRecord getRecorder() {
        return recorder;
    }

    public void start() {
        if (encoder == null)
            setupEncoder();
        encoder.start();

        if (recorder == null || micRecorder == null)
            setupRecorder();
        recorder.startRecording();
        micRecorder.start();

        thread.start();

        shouldEncode = true;
    }

    private void stop() {
        shouldEncode = false;
    }

    public boolean canEncode() {
        return input.size() >= 1;
    }

    public boolean canDecode() {
        return output.size() >= 1;
    }

    public byte[] getFrame() {
        byte[] frame;
        synchronized (lock) {
            try {
                frame = output.take();
            } catch (InterruptedException e) {
                frame = null;
            }
        }

        return frame;
    }

    public void pushInputFrame(byte[] frame) {
        if (videoEncoder == null) {
            return;
        }

        if (input.size() >= inputSize || !videoEncoder.isReadyToDecode)
            return;

        synchronized (lock) {
            input.offer(frame);
        }
    }

    private void pushOutputFrame(byte[] frame) {
        if (output.size() >= outputSize)
            return;

        synchronized (lock) {
            output.offer(frame);
        }
    }

    private synchronized void encodeFrame() {
        if (canEncode()) {
            byte[] frame;

            try {
                frame = input.take();
            } catch (InterruptedException e) {
                Log.e(TAG, "An error occured while taking a frame from input queue: " + e.toString());
                return;
            }

            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();

            int inputBufferIndex = encoder.dequeueInputBuffer(-1);

            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();

                inputBuffer.put(frame);

                encoder.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);

            byte[] outFrame;

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                int outSize = bufferInfo.size + 7; // ADTS HEADER SIZE ADDED

                outFrame = new byte[outSize];
                addADTStoPacket(outFrame, outSize);
                outputBuffer.get(outFrame, 7, bufferInfo.size);
                outputBuffer.position(bufferInfo.offset);

                pushOutputFrame(outFrame);

                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        // http://wiki.multimedia.cx/index.php?title=ADTS
        // MPEG Version: MPEG-2
        // MPEG-4 Audio Object Type: AAC-LC(2) - 1
        // MPEG-4 Sampling Frequency Index: 11(8000Hz)
        // MPEG-4 Channel Configuration: 1(Channel Count, mono)

        int profile = 1;  //AAC LC
        //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        int freqIdx = 11;  //44.1KHz
        int chanCfg = CHANNEL_COUNT;  //CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    @Override
    public void run() {
        while (shouldEncode) {
            encodeFrame();
        }
    }
}
