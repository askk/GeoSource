package com.honsal.geosource;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.honsal.geosource.camera.GeoCamera;
import com.honsal.geosource.encoders.AudioEncoder;
import com.honsal.geosource.encoders.VideoEncoder;
import com.honsal.geosource.etc.GPSInfo;
import com.honsal.geosource.etc.ToastOnUIThread;
import com.honsal.geosource.socket.GeoSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class GeoSource extends AppCompatActivity {

    private TextureView cameraPreview;
    private GeoCamera camera;
    private GeoSocket socket;
    private GPSInfo gpsInfo;
    private VideoEncoder videoEncoder;
    private AudioEncoder audioEncoder;
    private String domain;
    private int port;

    private EditText editIP, editPort, editId, editPw, editFramerate, editIframerate;
    private Button btnStart, btnStop, btnRemoveSettings;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (socket != null) {
            socket.closeLiveView();
            socket.logout();
        }

        ToastOnUIThread.makeTextLongExit(getBaseContext(), "어플리케이션을 종료합니다.");
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        socket.openLiveView();
//        socket.startSendingFrame();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (socket != null) {
//            socket.stopSendingFrame();
//            socket.closeLiveView();
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_geo_source);

        editIP = (EditText) findViewById(R.id.editIpAddress);
        editPort = (EditText) findViewById(R.id.editPort);
        editId = (EditText) findViewById(R.id.editID);
        editPw = (EditText) findViewById(R.id.editPw);
        editFramerate = (EditText) findViewById(R.id.editFramerate);
        editIframerate = (EditText) findViewById(R.id.editIframeinterval);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnRemoveSettings = (Button) findViewById(R.id.btnRemoveSettings);

        try {
//            FileInputStream fis = new FileInputStream(f);
            File f = new File(getFilesDir().getPath() + "/settings");

            if (f.exists()) {

                FileInputStream fis = openFileInput("settings");
                InputStreamReader isr = new InputStreamReader(fis);

                char[] content = new char[(int)f.length()];
                StringBuilder sb = new StringBuilder();
                int len = 0;

                while ((len = isr.read(content)) > 0) {
                    sb.append(new String(content));
                }

                isr.close();
                fis.close();

                String[] settings = sb.toString().split("\n");
                editIP.setText(settings[0]);
                editPort.setText(settings[1]);
                editId.setText(settings[2]);
                editPw.setText(settings[3]);
                editFramerate.setText(settings[4]);
                editIframerate.setText(settings[5]);
            }
        } catch (IOException e) {
            Log.w(this.getClass().getSimpleName(), "Cannot read setting file: " + e.toString());
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.w(this.getClass().getSimpleName(), "Cannot expect file size: " + e.toString());
        }

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                domain = editIP.getText().toString();
                port = Integer.parseInt(editPort.getText().toString());

//                String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sphonser";
//                File f = new File(dir, "settings");
//                if (!f.exists()) {
//                    try {
//                        f.createNewFile();
//                    } catch (IOException e) {
//                        Log.e(this.getClass().getSimpleName(), "CANNOT CREATE SETTING FILE");
//                    }
//                }
                try {
//                    FileOutputStream fos = new FileOutputStream(f, false);
                    FileOutputStream fos = openFileOutput("settings", MODE_PRIVATE);
                    OutputStreamWriter osw = new OutputStreamWriter(fos);
                    osw.write(editIP.getText().toString() + "\n");
                    osw.write(editPort.getText().toString() + "\n");
                    osw.write(editId.getText().toString() + "\n");
                    osw.write(editPw.getText().toString() + "\n");
                    osw.write(editFramerate.getText().toString() + "\n");
                    osw.write(editIframerate.getText().toString());
                    osw.flush();
                    osw.close();
                    fos.close();
                } catch (IOException e) {
                    Log.e(this.getClass().getSimpleName(), "CANNOT SAVE SETTING FILE");
                }

                setupEverything(editId.getText().toString(), editPw.getText().toString(), Integer.parseInt(editFramerate.getText().toString()), Integer.parseInt(editIframerate.getText().toString()));
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnRemoveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(getFilesDir().getPath() + "/settings");
                Log.d(this.getClass().getSimpleName(), "DELETION: " + Boolean.toString(f.delete()));
            }
        });

        ToastOnUIThread.setMainActivity(this);

        cameraPreview = (TextureView) findViewById(R.id.cameraPreview);

        camera = new GeoCamera(this, cameraPreview);
        gpsInfo = new GPSInfo(this);
        if (!gpsInfo.canGetLocation()) {
            gpsInfo.showSettingsAlert();
        }
    }

    private void setupEverything(String id, String pw, int framerate, int iframeinterval) {
        videoEncoder = new VideoEncoder(300 * 1024, 1280, 720, MediaFormat.MIMETYPE_VIDEO_AVC, framerate, iframeinterval, this);
        videoEncoder.start();

        audioEncoder = new AudioEncoder(64 * 1024, MediaFormat.MIMETYPE_AUDIO_AAC, 1, 8000, MediaCodecInfo.CodecProfileLevel.AACObjectLC, 0, AudioFormat.ENCODING_PCM_16BIT, this);
        audioEncoder.setVideoEncoder(videoEncoder);
        audioEncoder.start();

        socket = new GeoSocket(this, domain, port, videoEncoder, audioEncoder, gpsInfo);

        camera.setEncoder(videoEncoder);

        socket.start();
        socket.login(id, pw);
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
