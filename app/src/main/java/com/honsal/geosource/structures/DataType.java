package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class DataType {
    public static final int FRM_INTRA_CODE = 0x01;
    public static final int FRM_INTER_CODE = 0x02;
    public static final int FRM_VIDEO_CODE = 0x03;   // (FRM_INTRA_CODE | FRM_INTER_CODE)
    public static final int FRM_AUDIO1_CODE = 0x04;
    public static final int FRM_AUDIO2_CODE = 0x08;
    public static final int FRM_AUDIO_CODE = 0x0C;  // (FRM_AUDIO1_CODE | FRM_AUDIO2_CODE)
    public static final int FRM_GPS_CODE = 0x10;
    public static final int FRM_TEXT_CODE = 0x20;
    public static final int FRM_INDEX_CODE = 0x40;
    public static final int FRM_HEADER_CODE = 0x80;
}
