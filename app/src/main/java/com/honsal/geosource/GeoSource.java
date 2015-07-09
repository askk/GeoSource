package com.honsal.geosource;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;

import com.honsal.geosource.camera.GeoCamera;
import com.honsal.geosource.encoders.AudioEncoder;
import com.honsal.geosource.encoders.VideoEncoder;
import com.honsal.geosource.etc.GPSInfo;
import com.honsal.geosource.etc.ToastOnUIThread;
import com.honsal.geosource.socket.GeoSocket;

public class GeoSource extends AppCompatActivity {

    private TextureView cameraPreview;
    private GeoCamera camera;
    private GeoSocket socket;
    private GPSInfo gpsInfo;
    private VideoEncoder videoEncoder;
    private AudioEncoder audioEncoder;
    private String domain;
    private int port;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (socket != null) {
            socket.closeLiveView();
            socket.logout();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        socket.openLiveView();
        socket.startSendingFrame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (socket != null) {
            socket.stopSendingFrame();
            socket.closeLiveView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_geo_source);

        ToastOnUIThread.setMainActivity(this);

        cameraPreview = (TextureView) findViewById(R.id.cameraPreview);


        domain = "honsal.dyndns.info";
        port = 3217;

        setupEverything();
    }

    private void setupEverything() {
        camera = new GeoCamera(this, cameraPreview);
        gpsInfo = new GPSInfo(this);
        if (!gpsInfo.canGetLocation()) {
            gpsInfo.showSettingsAlert();
        }
        videoEncoder = new VideoEncoder(300 * 1024, 1280, 720, MediaFormat.MIMETYPE_VIDEO_AVC, 15, 1, this);
        videoEncoder.start();

        audioEncoder = new AudioEncoder(64 * 1024, MediaFormat.MIMETYPE_AUDIO_AAC, 1, 8000, MediaCodecInfo.CodecProfileLevel.AACObjectLC, 0, AudioFormat.ENCODING_PCM_16BIT, this);
        audioEncoder.start();

        socket = new GeoSocket(this, domain, port, videoEncoder, audioEncoder, gpsInfo);

        camera.setEncoder(videoEncoder);

        socket.start();

        socket.login("aaaa", "aaaa");
        socket.openLiveView();
        socket.startSendingFrame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_geo_source, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
