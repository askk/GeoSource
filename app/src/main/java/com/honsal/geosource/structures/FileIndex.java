package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class FileIndex {
    public FrameInfo info = null;
    public int count = 0;
    public int block = 0;
    public FrameInfo[] index = null;

    public FileIndex(FrameInfo info, int count, int block, FrameInfo[] index) {
        this.info = info;
        this.count = count;
        this.block = block;
        this.index = index;
    }
}
