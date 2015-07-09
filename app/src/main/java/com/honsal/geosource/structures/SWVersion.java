package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class SWVersion {
    public int version = 0;
    private byte major = 0;
    private byte minor = 0;
    private byte[] patch = new byte[2];

    public SWVersion(byte major, byte minor, byte[] patch) {
        int _patch = 0 << 16 | (patch[0] + patch[1]);
        _patch = _patch << 8 | minor;
        _patch = _patch << 8 | major;

        version = _patch;
    }
}
