package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class PlayIndex {
    int codeSize = 0;
    int msec = 0;
    long offset = 0;

    public PlayIndex(int code, int size, int msec, int offset) {
        this.codeSize = size << 16 | code;
        this.msec = msec;
        this.offset = offset;
    }
}
