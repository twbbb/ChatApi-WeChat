package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXLink extends WXMessage implements Serializable, Cloneable {
    /**
     * 链接标题
     */
    public String linkName;
    /**
     * 链接地址
     */
    public String linkUrl;
}
