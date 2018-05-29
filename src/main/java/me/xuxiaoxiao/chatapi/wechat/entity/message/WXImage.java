package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.File;
import java.io.Serializable;

public class WXImage extends WXMessage implements Serializable, Cloneable {
    /**
     * 图片宽度
     */
    public int imgWidth;
    /**
     * 图片高度
     */
    public int imgHeight;
    /**
     * 静态图消息中的缩略图，动态图消息中的原图
     */
    public File image;
    /**
     * 静态图刚开始为null，手动调用接口后为原图。动态图一开始就是原图
     */
    public File origin;
}
