package com.honsal.geosource.structures;

/**
 * Created by 이 on 2015-06-12.
 */
public class NetworkProtocol {
    public static final int START_CODE = 0xABCDCDAB;

    public static final int NET_ALIVE = 0x00010000;
    public static final int NET_LOGIN = 0x00010001;
    public static final int NET_LOGOUT = 0x00010002;
    public static final int NET_GET_CONFIG = 0x00010003;
    public static final int NET_SET_CONFIG = 0x00010004;
    public static final int NET_SYNC_TIME = 0x00010005;

    public static final int NET_LIVE_LIST = 0x00020001;
    public static final int NET_LIVE_OPEN = 0x00020002;
    public static final int NET_LIVE_CLOSE = 0x00020003;
    public static final int NET_LIVE_FRAME = 0x00020011;
    public static final int NET_LIVE_VIDEO = 0x00020012;
    public static final int NET_LIVE_AUDIO = 0x00020013;
    public static final int NET_LIVE_GPS = 0x00020014;
    public static final int NET_LIVE_TEXT = 0x00020015;

    public static final int NET_FILE_LIST = 0x00040001;
    public static final int NET_FILE_OPEN = 0x00040002;
    public static final int NET_FILE_CLOSE = 0x00040003;
    public static final int NET_FILE_INDEX = 0x00040004;
    public static final int NET_FILE_FIND = 0x00040005;
    public static final int NET_FILE_DELETE = 0x00040006;
    public static final int NET_PLAY_FRAME = 0x00040011;
    public static final int NET_PLAY_VIDEO = 0x00040012;
    public static final int NET_PLAY_AUDIO = 0x00040013;
    public static final int NET_PLAY_GPS = 0x00040014;
    public static final int NET_PLAY_TEXT = 0x00040015;
    public static final int NET_DOWNLOAD = 0x00040021;
}
