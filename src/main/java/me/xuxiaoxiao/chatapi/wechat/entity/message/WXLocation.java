package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.Serializable;

public class WXLocation extends WXMessage implements Serializable, Cloneable {
    /**
     * 地点名称
     */
    public String locationName;
    /**
     * 地点地图图片
     */
    public String locationImage;
    /**
     * 地点的腾讯地图url
     */
    public String locationUrl;
}
