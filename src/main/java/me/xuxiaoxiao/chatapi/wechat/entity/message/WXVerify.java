package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXVerify extends WXMessage implements Serializable, Cloneable {
    public String userId;
    public String userName;
    public String signature;
    public String province;
    public String city;
    public int gender;
    public int verify;
    public String ticket;
}
