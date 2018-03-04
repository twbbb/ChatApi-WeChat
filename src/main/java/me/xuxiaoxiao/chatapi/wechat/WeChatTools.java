package me.xuxiaoxiao.chatapi.wechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.xuxiaoxiao.chatapi.wechat.entity.AddMsg;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.XOption;
import me.xuxiaoxiao.xtools.common.http.XRequest;

import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 模拟网页微信客户端工具类
 */
public class WeChatTools {
    /**
     * 文字消息
     */
    public static final int TYPE_TEXT = 1;
    /**
     * 图片消息
     */
    public static final int TYPE_IMAGE = 3;
    /**
     * 语音消息
     */
    public static final int TYPE_VOICE = 34;
    /**
     * 好友请求
     */
    public static final int TYPE_VERIFY = 37;
    /**
     * 名片消息
     */
    public static final int TYPE_CARD = 42;
    /**
     * 视频消息
     */
    public static final int TYPE_VIDEO = 43;
    /**
     * 收藏的表情
     */
    public static final int TYPE_FACE = 47;
    /**
     * 转账、文件、链接、笔记等
     */
    public static final int TYPE_OTHER = 49;

    /**
     * 消息已读
     */
    static final int TYPE_NOTIFY = 51;
    /**
     * 系统消息
     */
    static final int TYPE_SYSTEM = 10000;

    static final Logger LOGGER = Logger.getLogger("me.xuxiaoxiao.chatapi.wechat");
    static final Gson GSON = new GsonBuilder().create();
    static final XOption HTTP_OPTION = new XOption(90 * 1000, 90 * 1000);

    private WeChatTools() {
    }

    /**
     * 判断获取到的消息是否是群消息
     *
     * @param addMsg 获取到的消息
     * @return 是否是群消息
     */
    public static boolean isGroupMsg(AddMsg addMsg) {
        return addMsg.FromUserName.startsWith("@@");
    }

    /**
     * 获取到的消息的实际发送者
     *
     * @param addMsg 获取到的消息
     * @return 消息的实际发送者的UserName
     */
    public static String msgSender(AddMsg addMsg) {
        if (isGroupMsg(addMsg)) {
            return addMsg.Content.substring(0, addMsg.Content.indexOf(':'));
        } else {
            return addMsg.FromUserName;
        }
    }

    /**
     * 获取到的消息的实际内容
     *
     * @param addMsg 获取到的消息
     * @return 消息的实际内容
     */
    public static String msgContent(AddMsg addMsg) {
        if (isGroupMsg(addMsg)) {
            return addMsg.Content.substring(addMsg.Content.indexOf(':') + ":<br/>".length());
        } else {
            return addMsg.Content;
        }
    }

    /**
     * 接口请求
     *
     * @param regex 返回值需要满足的正则表达式
     * @return 接口返回的字符串
     */
    static String request(XRequest request, String regex) {
        for (int i = 0; i < 3; i++) {
            String respStr = XTools.http(HTTP_OPTION, request).string();
            if (!XTools.strEmpty(respStr) && Pattern.compile(regex).matcher(respStr).find()) {
                WeChatTools.LOGGER.finest(String.format("请求接口返回数据：\n%s", respStr));
                return respStr;
            }
        }
        return null;
    }
}
