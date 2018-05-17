package me.xuxiaoxiao.chatapi.wechat.entity.message;

import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXContact;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXGroup;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;

import java.io.Serializable;

public abstract class WXMessage implements Serializable, Cloneable {
    /**
     * 消息的id
     */
    public long id;
    /**
     * 消息的时间戳
     */
    public long timestamp;
    /**
     * 消息来源的群，如果不是群消息，值为null
     */
    public WXGroup fromGroup;
    /**
     * 消息来源的用户
     */
    public WXUser fromUser;
    /**
     * 消息发送的联系人
     */
    public WXContact toContact;
    /**
     * 消息的内容
     */
    public String content;
}
