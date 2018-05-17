package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXRevoke extends WXMessage implements Serializable, Cloneable {
    public long msgId;
    public String msgReplace;
}
