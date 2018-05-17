package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXRecommend extends WXMessage implements Serializable, Cloneable {
    public String userId;
    public String userName;
    public int gender;
    public String signature;
    public String province;
    public String city;
    public int verifyFlag;
}
