package me.xuxiaoxiao.chatapi.wechat.entity.contact;

import java.io.Serializable;

public abstract class WXContact implements Serializable, Cloneable {
    public static final int CONTACT = 1;
    public static final int CONTACT_CHAT = 2;
    public static final int CONTACT_CHATROOM = 4;
    public static final int CONTACT_BLACKLIST = 8;
    public static final int CONTACT_DOMAIN = 16;
    public static final int CONTACT_HIDE = 32;
    public static final int CONTACT_FAVOUR = 64;
    public static final int CONTACT_3RDAPP = 128;
    public static final int CONTACT_SNSBLACKLIST = 256;
    public static final int CONTACT_NOTIFYCLOSE = 512;
    public static final int CONTACT_TOP = 2048;

    /**
     * 账户id，以@@开头的是群组，以@开头的是普通用户，其他的是特殊用户比如文件助手等
     */
    public String id;
    /**
     * 账户的名称
     */
    public String name;
    /**
     * 账户名称的拼音的首字母
     */
    public String namePY;
    /**
     * 账户名称的拼音全拼
     */
    public String nameQP;
    /**
     * 账户头像
     */
    public String avatar;
    /**
     * 联系人标志字段
     */
    public int contactFlag;
}
