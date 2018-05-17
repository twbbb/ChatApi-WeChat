package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXNotify extends WXMessage implements Serializable, Cloneable {
    public static final int NOTIFY_READED = 1;
    public static final int NOTIFY_ENTER_SESSION = 2;
    public static final int NOTIFY_INITED = 3;
    public static final int NOTIFY_SYNC_CONV = 4;
    public static final int NOTIFY_QUIT_SESSION = 5;

    public int notifyCode;
    public String notifyContact;
}
