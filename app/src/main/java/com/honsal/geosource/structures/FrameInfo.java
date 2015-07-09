package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class FrameInfo {
    public int codeSize = 0;
    public int time = 0;

    public FrameInfo(int code, int size, int time) {
        codeSize = size << 8 | code;
        this.time = time;
    }

    public FrameInfo(int code, int size, long time) {
        this(code, size, (int)(time / 1000));
    }
}
