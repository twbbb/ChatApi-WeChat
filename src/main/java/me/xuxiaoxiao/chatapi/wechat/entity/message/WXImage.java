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
     * 图片文件，一开始是缩略图，手动调用接口可获取大图（动态图一开始就是大图，无需再获取）
     */
    public File image;
}
