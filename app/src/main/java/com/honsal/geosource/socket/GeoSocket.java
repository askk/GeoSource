package com.honsal.geosource.socket;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.honsal.geosource.encoders.AudioEncoder;
import com.honsal.geosource.encoders.VideoEncoder;
import com.honsal.geosource.etc.GPSInfo;
import com.honsal.geosource.etc.GeomexRC4;
import com.honsal.geosource.etc.ToastOnUIThread;
import com.honsal.geosource.structures.DataType;
import com.honsal.geosource.structures.FrameInfo;
import com.honsal.geosource.structures.NetworkProtocol;
import com.honsal.geosource.structures.SWVersion;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Created by 이 on 2015-07-08.
 */
public class GeoSocket implements Runnable {
    private static final String TAG = "GeoSocket";
    private static final int SOCKET_CONNECT_TIMEOUT = 10000;
    private static final int IO_BUFFER_SIZE = 8192;
    private static final int FLAG_LOGIN = 0x01;
    private static final int FLAG_LIVE_OPEN = 0x02;
    private static final int FLAG_LIVE_FRAME = 0x04;
    private static final int FLAG_LIVE_CLOSE = 0x08;
    private static final int FLAG_EXIT = 0x10;
    private static final int FLAG_LOGOUT = 0x10;
    private Context context;
    private String domain;
    private int port;
    private long macAddress;
    private VideoEncoder videoEncoder;
    private AudioEncoder audioEncoder;
    private GPSInfo gpsInfo;
    private Socket socket;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;
    private String id;
    private String pw;
    private int flag = 0x00;

    private InetAddress inetAddress;

    private Thread getHostAddressThread;

    private Thread thread;

    public GeoSocket(Context context, String domain, int port, VideoEncoder videoEncoder, AudioEncoder audioEncoder, GPSInfo gpsInfo) {
        this.context = context;
        this.domain = domain;
        this.port = port;
        this.videoEncoder = videoEncoder;
        this.audioEncoder = audioEncoder;
        this.gpsInfo = gpsInfo;
        getMacAddress();
        getHostAddress();
    }

    private void getMacAddress() {
        try {
            // Mac address를 찾음.
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String macInfo = wifiInfo.getMacAddress();

            macInfo = macInfo.replace(":", "");

            // 찾은 Mac address를 HEX형식으로 변환해 long형식 변수에 담음 (macAddrLong)
            String macAddr = "";

            for (int i = 0; i <= 12; i++) {
                if (i % 2 == 0 && i > 0) {
                    macAddr += macInfo.substring((12 - i), (12 - i + 2));
                }
            }

            macAddress = Long.parseLong(macAddr, 16);
        } catch (Exception e) {
            Log.e(TAG, "Cannot resolve mac address: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "Cannot resolve mac address: " + e.toString());
        }
    }

    private void getHostAddress() {
        Log.d(TAG, "Getting host address...");
        Toast.makeText(context, "Getting host address...", Toast.LENGTH_LONG);
        getHostAddressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    inetAddress = InetAddress.getByName(domain);
                } catch (UnknownHostException e) {
                    Log.e(TAG, "Unknown host");
                    ToastOnUIThread.makeTextLongExit(context, "Unknown host");
                }
            }
        }, "GetHostThread");

        getHostAddressThread.start();
        try {
            getHostAddressThread.join();
        } catch (InterruptedException e) {
            Log.d(TAG, "An interrupt exception occured while getting host address: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "An interrupt exception occured while getting host address: " + e.toString());
        }

        Toast.makeText(context, "HostAddress: " + inetAddress.getHostAddress(), Toast.LENGTH_LONG);
        Log.d(TAG, "HostAddress: " + inetAddress.getHostAddress());
    }

    public void start() {
//        videoEncoder.start();
//        audioEncoder.start();
        thread = new Thread(this, "GeoSocketThread");
        thread.start();
    }

    private void readySocket() {
        try {
            socket = new Socket();
//            socket.setSoTimeout(SOCKET_CONNECT_TIMEOUT);
            socket.connect(new InetSocketAddress(inetAddress, port));
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Socket timed out: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "Socket timed out: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "An error occured while initializing socket: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "An error occured while initializing socket: " + e.toString());
        }

        if (!socket.isConnected()) {
            Log.e(TAG, "Socket was not connected.");
            ToastOnUIThread.makeTextLongExit(context, "Socket was not connected.");
        }

        try {
            bis = new BufferedInputStream(socket.getInputStream(), IO_BUFFER_SIZE);
            bos = new BufferedOutputStream(socket.getOutputStream(), IO_BUFFER_SIZE);
        } catch (IOException e) {
            Log.e(TAG, "Cannot open I/O stream: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "Cannot open I/O stream: " + e.toString());
        }

    }

    public void reopen() {
        readySocket();
    }

    private byte[] makePacket(int command, int size, @Nullable byte[] data) {
        ByteBuffer packet = ByteBuffer.allocate(size + 12);
        packet.order(ByteOrder.LITTLE_ENDIAN);
        packet.putInt(NetworkProtocol.START_CODE);
        packet.putInt(command);
        packet.putInt(size);
        if (data != null) {
            packet.put(data);
        }

        return packet.array();
    }

    private void sendPacket(byte[] packet) {
        if (packet == null) {
            return;
        }
        try {
            bos.write(packet);
            bos.flush();
        } catch (IOException e) {
            Log.e(TAG, "An error occured while sending packet to server; packet: " + packet.toString() + ", error: " + e.toString());
            ToastOnUIThread.makeTextLong(context, "An error occured while sending packet to server; packet: " + packet.toString() + ", error: " + e.toString());
        }
    }

    private boolean checkResponse(int code, @Nullable byte[] data) {
        try {
            while (!(bis.read() == 0xAB && bis.read() == 0xCD && bis.read() == 0xCD && bis.read() == 0xAB)) {
            }
        } catch (IOException e) {
            Log.e(TAG, "An error occured while skipping bytes for find start code: " + e.toString());
            ToastOnUIThread.makeTextLong(context, "An error occured while skipping bytes for find start code: " + e.toString());
        }

        byte[] inCode = new byte[4];
        try {
            bis.read(inCode, 0, 4);
            if (ByteBuffer.wrap(inCode).order(ByteOrder.LITTLE_ENDIAN).getInt() == code) {
                if (data == null) {
                    return true;
                } else {
                    int len = (data == null ? 0 : data.length);
                    byte[] inData = new byte[len];
                    bis.read(inData, 0, len);
                    if (ByteBuffer.wrap(inData).order(ByteOrder.LITTLE_ENDIAN).array() == data) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "An error occured while reading response data: " + e.toString());
            ToastOnUIThread.makeTextLong(context, "An error occured while reading response data: " + e.toString());
            return false;
        }
        return false;
    }

    public void login(String id, String pw) {
        this.id = id;
        this.pw = pw;
        flag |= FLAG_LOGIN;
    }

    private void _login() {
        ByteBuffer loginPacket = ByteBuffer.allocate(128);
        loginPacket.order(ByteOrder.LITTLE_ENDIAN);
        loginPacket.put(id.getBytes(StandardCharsets.UTF_16LE));
        loginPacket.position(64);
        loginPacket.put(pw.getBytes(StandardCharsets.UTF_16LE));

        sendPacket(makePacket(NetworkProtocol.NET_LOGIN, loginPacket.array().length, GeomexRC4.encrypt(loginPacket.array())));

        if (checkResponse(NetworkProtocol.NET_LOGIN, null)) {
            ToastOnUIThread.makeTextLong(context, "로그인 성공!");
        } else {
            ToastOnUIThread.makeTextLongExit(context, "로그인 실패.");
        }
    }

    public void openLiveView() {
        flag |= FLAG_LIVE_OPEN;
    }

    private void live_open() {
        int size = 128;

        ByteBuffer liveopenPacket = ByteBuffer.allocate(size);
        liveopenPacket.order(ByteOrder.LITTLE_ENDIAN);

        FrameInfo info = new FrameInfo(DataType.FRM_HEADER_CODE, size, System.currentTimeMillis());
        SWVersion version = new SWVersion((byte) 1, (byte) 0, new byte[]{0, 0});

        liveopenPacket.putInt(info.codeSize);
        liveopenPacket.putInt(info.time);
        liveopenPacket.putInt(version.version);
        liveopenPacket.putInt(0);
        liveopenPacket.putLong(macAddress);
        liveopenPacket.putInt(0); // videoType
        liveopenPacket.putInt(1280); // videoWidth
        liveopenPacket.putInt(720); // videoHeight
        liveopenPacket.putInt(0); // audioType
        liveopenPacket.putInt(8000); // audioSamplingRate
        liveopenPacket.putFloat(gpsInfo.getLatitude());
        liveopenPacket.putFloat(gpsInfo.getLongitude());
        liveopenPacket.putFloat(gpsInfo.getDirection());

        sendPacket(makePacket(NetworkProtocol.NET_LIVE_OPEN, size, liveopenPacket.array()));

        if (checkResponse(NetworkProtocol.NET_LIVE_OPEN, null)) {
            ToastOnUIThread.makeTextLong(context, "라이브 뷰 오픈 성공!");
        } else {
            ToastOnUIThread.makeTextLongExit(context, "라이브 뷰 오픈 실패.");
        }
    }

    public void startSendingFrame() {
        flag |= FLAG_LIVE_FRAME;
    }

    private synchronized void live_frame() {
        if (!videoEncoder.canDecode())
            return;

        byte[] frame = videoEncoder.getFrame();

        int frameSize = frame.length + 8;

        ByteBuffer packet = ByteBuffer.allocate(frameSize);
        packet.order(ByteOrder.LITTLE_ENDIAN);

        int code = frame[4];

        FrameInfo frameInfo = new FrameInfo((code == 0x67 ? DataType.FRM_VIDEO_CODE : (code == 0x65 ? DataType.FRM_INTRA_CODE : DataType.FRM_INTER_CODE)), frameSize, System.currentTimeMillis());

        packet.putInt(frameInfo.codeSize);
        packet.putInt(frameInfo.time);
        packet.put(frame);

        if (code == 0x65) {
            live_gps();
        }

        sendPacket(makePacket(NetworkProtocol.NET_LIVE_FRAME, frameSize, packet.array()));
    }

    private synchronized void live_audio() {
        if (!audioEncoder.canDecode())
            return;

        byte[] frame = audioEncoder.getFrame();

        int frameSize = frame.length + 8;

        ByteBuffer packet = ByteBuffer.allocate(frameSize);
        packet.order(ByteOrder.LITTLE_ENDIAN);

        FrameInfo frameInfo = new FrameInfo(DataType.FRM_AUDIO1_CODE, frameSize, System.currentTimeMillis());

        packet.putInt(frameInfo.codeSize);
        packet.putInt(frameInfo.time);
        packet.put(frame);

        sendPacket(makePacket(NetworkProtocol.NET_LIVE_FRAME, frameSize, packet.array()));
    }

    private void live_gps() {
        int size = 20; // 8 + la, lo, dir

        ByteBuffer packet = ByteBuffer.allocate(size);
        packet.order(ByteOrder.LITTLE_ENDIAN);

        FrameInfo frameInfo = new FrameInfo(DataType.FRM_GPS_CODE, size, System.currentTimeMillis());

        packet.putInt(frameInfo.codeSize);
        packet.putInt(frameInfo.time);
        packet.putFloat(gpsInfo.getLatitude());
        packet.putFloat(gpsInfo.getLongitude());
        packet.putFloat(gpsInfo.getDirection());

        sendPacket(makePacket(NetworkProtocol.NET_LIVE_FRAME, size, packet.array()));
    }

    public void closeLiveView() {
        flag |= FLAG_LIVE_CLOSE;
    }

    private void live_close() {
        sendPacket(makePacket(NetworkProtocol.NET_LIVE_CLOSE, 0, null));
        if (checkResponse(NetworkProtocol.NET_LIVE_CLOSE, null)) {
            ToastOnUIThread.makeTextLong(context, "라이브 뷰 클로즈 성공!");
        } else {
            ToastOnUIThread.makeTextLong(context, "라이브 뷰 클로즈 실패.");
        }
    }

    public void logout() {
        flag |= FLAG_LOGOUT;
    }

    private void _logout() {
        sendPacket(makePacket(NetworkProtocol.NET_LOGOUT, 0, null));
        flag &= FLAG_EXIT;
    }

    public void stopSendingFrame() {
        flag &= (0xFF - FLAG_LIVE_FRAME);
    }

    @Override
    public void run() {
        readySocket();
        while ((flag & FLAG_EXIT) == 0) {
            if ((flag & FLAG_LOGIN) != 0) {
                _login();
                flag &= 0xFF - FLAG_LOGIN;
            }

            if ((flag & FLAG_LIVE_OPEN) != 0) {
                live_open();
                flag &= 0xFF - FLAG_LIVE_OPEN;
            }

            if ((flag & FLAG_LIVE_FRAME) != 0) {
                live_frame();
                live_audio();
            }

            if ((flag & FLAG_LIVE_CLOSE) != 0) {
                live_close();
                flag &= 0xFF - FLAG_LIVE_CLOSE;
            }

            if ((flag & FLAG_LOGOUT) != 0) {
                _logout();
                flag &= 0xFF - FLAG_LOGOUT;
            }
        }

        try {
            socket.close();
            bis.close();
            bos.close();
        } catch (IOException e) {

        }
    }
}
