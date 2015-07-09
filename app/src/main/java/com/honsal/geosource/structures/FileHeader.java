package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class FileHeader {
    public FrameInfo info = null;
    public SWVersion version = null;
    public MacInfo macAddress = null;
    public int videoType = 0;
    public int videoWidth = 0;
    public int videoHeight = 0;
    public int audioType = 0;
    public int audioSampling = 0;
    public float latitude = 0.0f;
    public float longitude = 0.0f;
    public float direction = 0.0f;
    public byte[] reserved = null;

    public FileHeader(FrameInfo info, SWVersion version, MacInfo macAddress, int videoType, int videoWidth, int videoHeight, int audioType, int audioSampling, float latitude, float longitude, float direction) {
        this.info = info;
        this.version = version;
        this.macAddress = macAddress;
        this.videoType = videoType;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.audioType = audioType;
        this.audioSampling = audioSampling;
        this.latitude = latitude;
        this.longitude = longitude;
        this.direction = direction;
        this.reserved = new byte[72];
    }
}
