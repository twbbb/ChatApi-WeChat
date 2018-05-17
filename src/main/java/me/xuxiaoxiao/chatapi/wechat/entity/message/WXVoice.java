package me.xuxiaoxiao.chatapi.wechat.entity.message;

import java.io.File;
import java.io.Serializable;

public class WXVoice extends WXMessage implements Serializable, Cloneable {
    /**
     * 语音长度，毫秒
     */
    public long voiceLength;
    /**
     * 语音文件
     */
    public File voice;
}
