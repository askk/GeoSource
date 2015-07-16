package com.honsal.geosource.encoders;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import com.honsal.geosource.etc.ToastOnUIThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by 이 on 2015-07-08.
 */

public class VideoEncoder implements Runnable {
    private static final String TAG = "VideoEncoder";
    private final int BIT_RATE;
    private final int WIDTH;
    private final int HEIGHT;
    private final String MIME;
    private final int FRAMERATE;
    private final int I_FRAMERATE_INTERVAL;
    private final int COLORFORMAT;
    private final Context CONTEXT;
    private MediaCodec encoder;
    private Thread thread;
    private Object lock = new Object();
    private boolean shouldEncode = false;
    private ArrayBlockingQueue<byte[]> input;
    private int inputSize = 60;
    private ArrayBlockingQueue<byte[]> output;
    private int outputSize = 60;
    public boolean isReadyToDecode = false;

    public VideoEncoder(int bitrate, int width, int height, String mime, int framerate, int i_frameinterval, Context context) {
        BIT_RATE = bitrate;
        WIDTH = width;
        HEIGHT = height;
        MIME = mime;
        FRAMERATE = framerate;
        I_FRAMERATE_INTERVAL = i_frameinterval;
        CONTEXT = context;

        MediaCodecInfo info = selectCodec();
        int colorformat = selectColorFormat(selectCodec());
        if (info != null && colorformat != -1)
            COLORFORMAT = colorformat;
        else
            COLORFORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;

        thread = new Thread(this, "GeoVideoEncodingThread");
    }

    private void setupEncoder() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME, WIDTH, HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAMERATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAMERATE_INTERVAL);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLORFORMAT);

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

    private MediaCodecInfo selectCodec() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            for (String type : codecInfo.getSupportedTypes()) {
                if (type.equalsIgnoreCase(MIME)) {
                    Log.i(TAG, "Codec selected: " + codecInfo.getName());
                    return codecInfo;
                }
            }
        }

        return null;
    }

    private int selectColorFormat(MediaCodecInfo info) {
        MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(MIME);

        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorformat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorformat)) {
                return colorformat;
            }
        }

        return -1;
    }

    private boolean isRecognizedFormat(int colorformat) {
        switch (colorformat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    public void start() {
        if (encoder == null)
            setupEncoder();

        encoder.start();
        thread.start();

        shouldEncode = true;
    }

    public void close() {
        shouldEncode = false;

        encoder.stop();
        encoder.release();
        encoder = null;
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
        if (input.size() >= inputSize)
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
                Log.e(TAG, "An error occured while take a frame from input queue: " + e.toString());
                return;
            }

            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();

            int inputBufferIndex = encoder.dequeueInputBuffer(-1);

            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();

//                int wh4 = frame.length / 6;
//
//                inputBuffer.put(frame, 0, wh4 * 4);
//                inputBuffer.put(frame, wh4 * 5, wh4);
//                inputBuffer.put(frame, wh4 * 4, wh4);

                inputBuffer.put(frame);

                encoder.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);

            Log.d(TAG, Integer.toString(outputBufferIndex));

            byte[] outFrame;

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                outFrame = new byte[bufferInfo.size];
                outputBuffer.get(outFrame);

                pushOutputFrame(outFrame);

                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
                isReadyToDecode = true;
            }
        }
    }

    @Override
    public void run() {
        while (shouldEncode) {
            encodeFrame();
        }
    }
}