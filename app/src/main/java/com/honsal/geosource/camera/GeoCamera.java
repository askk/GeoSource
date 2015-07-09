package com.honsal.geosource.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.TextureView;

import com.honsal.geosource.encoders.VideoEncoder;
import com.honsal.geosource.etc.ToastOnUIThread;

import java.io.IOException;

/**
 * Created by 이 on 2015-07-08.
 */
public class GeoCamera implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {
    private static final String TAG = "GeoCamera";

    private Context context;
    private TextureView textureView;

    private Camera camera;
    private VideoEncoder encoder;

    public GeoCamera(Context context, TextureView textureView) {
        this.context = context;

        textureView.setSurfaceTextureListener(this);

        this.textureView = textureView;
    }

    public void setEncoder(VideoEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        camera = Camera.open();
        if (camera == null) {
            Log.e(TAG, "Default camera is not available!");
            ToastOnUIThread.makeTextShortExit(context, "Default camera is not available!");
        }

        Camera.Parameters params = camera.getParameters();
        params.setPreviewFpsRange(15000, 15000);
        params.setPreviewFormat(ImageFormat.YV12);
        params.setPreviewSize(1280, 720);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        params.setRecordingHint(true);

        camera.setParameters(params);
        camera.setPreviewCallback(this);
        camera.setDisplayOrientation(90);

        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "An error occured while setup camera and preview: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "An error occured while setup camera and preview: " + e.toString());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        camera = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (encoder != null) {
            encoder.pushInputFrame(data);
        }
    }
}
