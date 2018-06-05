package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXContact;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXGroup;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;
import me.xuxiaoxiao.chatapi.wechat.entity.message.*;
import me.xuxiaoxiao.chatapi.wechat.protocol.*;
import me.xuxiaoxiao.xtools.common.XTools;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟网页微信客户端
 */
public final class WeChatClient {
    public static final String LOGIN_TIMEOUT = "登陆超时";
    public static final String LOGIN_EXCEPTION = "登陆异常";
    public static final String INIT_EXCEPTION = "初始化异常";
    public static final String LISTEN_EXCEPTION = "监听异常";

    private static final Logger LOGGER = Logger.getLogger("me.xuxiaoxiao.chatapi.wechat");
    private static final Pattern REX_GROUPMSG = Pattern.compile("(@[0-9a-zA-Z]+):<br/>([\\s\\S]*)");
    private static final Pattern REX_REVOKE_ID = Pattern.compile("&lt;msgid&gt;(\\d+)&lt;/msgid&gt;");
    private static final Pattern REX_REVOKE_REPLACE = Pattern.compile("&lt;replacemsg&gt;&lt;!\\[CDATA\\[([\\s\\S]*)\\]\\]&gt;&lt;/replacemsg&gt;");

    private final WeChatThread wxThread = new WeChatThread();
    private final WeChatContacts wxContacts = new WeChatContacts();
    private final WeChatApi wxAPI;
    private final WeChatListener wxListener;

    public WeChatClient(WeChatListener wxListener) {
        this(wxListener, null, null);
    }

    public WeChatClient(WeChatListener wxListener, File folder, Handler handler) {
        Objects.requireNonNull(wxListener);
        this.wxListener = wxListener;
        if (folder == null) {
            folder = new File("");
        }
        if (handler == null) {
            handler = new ConsoleHandler();
            handler.setLevel(Level.FINER);
        }
        this.wxAPI = new WeChatApi(folder);
        LOGGER.setLevel(handler.getLevel());
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(handler);
    }

    /**
     * 启动客户端，注意：一个客户端类的实例只能被启动一次
     */
    public void startup() {
        wxThread.start();
    }

    /**
     * 客户端是否正在运行
     *
     * @return 是否正在运行
     */
    public boolean isWorking() {
        return !wxThread.isInterrupted();
    }

    /**
     * 关闭客户端，注意：关闭后的客户端不能再被启动
     */
    public void shutdown() {
        wxAPI.webwxlogout();
        wxThread.interrupt();
    }

    /**
     * 获取当前登录的用户信息
     *
     * @return 当前登录的用户信息
     */
    public WXUser userMe() {
        return wxContacts.getMe();
    }

    /**
     * 根据userId获取用户好友
     *
     * @param userId 好友的id
     * @return 好友的信息
     */
    public WXUser userFriend(String userId) {
        return wxContacts.getFriend(userId);
    }

    /**
     * 获取用户所有好友
     *
     * @return 用户所有好友
     */
    public HashMap<String, WXUser> userFriends() {
        return wxContacts.getFriends();
    }

    /**
     * 根据群id获取群信息
     *
     * @param groupId 群id
     * @return 群信息
     */
    public WXGroup userGroup(String groupId) {
        return wxContacts.getGroup(groupId);
    }

    /**
     * 获取用户所有群
     *
     * @return 用户所有群
     */
    public HashMap<String, WXGroup> userGroups() {
        return wxContacts.getGroups();
    }

    /**
     * 根据联系人id获取用户联系人信息
     *
     * @param contactId 联系人id
     * @return 联系人信息
     */
    public WXContact userContact(String contactId) {
        return wxContacts.getContact(contactId);
    }

    /**
     * 发送文字消息
     *
     * @param contactId 目标联系人的id
     * @param text      文字内容
     */
    public void sendText(String contactId, String text) {
        LOGGER.info(String.format("向（%s）发送消息：%s", contactId, text));
        wxAPI.webwxsendmsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_TEXT, null, 0, text, null, wxContacts.getMe().id, contactId));
    }

    public void sendFile(String contactId, File file) {
        if (WeChatTools.fileSuffix(file).equals("mp4") && file.length() >= 20L * 1024L * 1024L) {
            LOGGER.warning(String.format("向（%s）发送的视频文件大于20M，无法发送", contactId));
        } else {
            try {
                LOGGER.info(String.format("向（%s）发送文件：%s", contactId, file.getAbsolutePath()));
                String mediaId = null, aesKey = null, signature = null;
                if (file.length() >= 25L * 1024L * 1024L) {
                    RspCheckUpload rspCheckUpload = wxAPI.webwxcheckupload(file, wxContacts.getMe().id, contactId);
                    mediaId = rspCheckUpload.MediaId;
                    aesKey = rspCheckUpload.AESKey;
                    signature = rspCheckUpload.Signature;
                }
                if (XTools.strEmpty(mediaId)) {
                    RspUploadMedia rspUploadMedia = wxAPI.webwxuploadmedia(wxContacts.getMe().id, contactId, file, aesKey, signature);
                    mediaId = rspUploadMedia.MediaId;
                }
                if (!XTools.strEmpty(mediaId)) {
                    switch (WeChatTools.fileType(file)) {
                        case "pic":
                            wxAPI.webwxsendmsgimg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_IMAGE, mediaId, null, "", signature, wxContacts.getMe().id, contactId));
                            break;
                        case "video":
                            wxAPI.webwxsendvideomsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_VIDEO, mediaId, null, "", signature, wxContacts.getMe().id, contactId));
                            break;
                        default:
                            if (WeChatTools.fileSuffix(file).equals("gif")) {
                                wxAPI.webwxsendemoticon(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_EMOJI, mediaId, 2, "", signature, wxContacts.getMe().id, contactId));
                            } else {
                                StringBuilder sbAppMsg = new StringBuilder();
                                sbAppMsg.append("<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''>");
                                sbAppMsg.append("<title>").append(file.getName()).append("</title>");
                                sbAppMsg.append("<des></des>");
                                sbAppMsg.append("<action></action>");
                                sbAppMsg.append("<type>6</type>");
                                sbAppMsg.append("<content></content>");
                                sbAppMsg.append("<url></url>");
                                sbAppMsg.append("<lowurl></lowurl>");
                                sbAppMsg.append("<appattach>");
                                sbAppMsg.append("<totallen>").append(file.length()).append("</totallen>");
                                sbAppMsg.append("<attachid>").append(mediaId).append("</attachid>");
                                sbAppMsg.append("<fileext>").append(XTools.strEmpty(WeChatTools.fileSuffix(file)) ? "undefined" : WeChatTools.fileSuffix(file)).append("</fileext>");
                                sbAppMsg.append("</appattach>");
                                sbAppMsg.append("<extinfo></extinfo>");
                                sbAppMsg.append("</appmsg>");
                                wxAPI.webwxsendappmsg(new ReqSendMsg.Msg(6, null, null, sbAppMsg.toString(), signature, wxContacts.getMe().id, contactId));
                            }
                            break;
                    }
                } else {
                    LOGGER.severe(String.format("向（%s）发送的文件发送失败", contactId));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 撤回消息
     *
     * @param contactId   目标用户的UserName
     * @param clientMsgId 本地消息id
     * @param serverMsgId 服务端消息id
     */
    public void revokeMsg(String contactId, String clientMsgId, String serverMsgId) {
        LOGGER.info(String.format("撤回向（%s）发送的消息：%s，%s", contactId, clientMsgId, serverMsgId));
        wxAPI.webwxrevokemsg(clientMsgId, serverMsgId, contactId);
    }

    /**
     * 获取图片消息的大图
     *
     * @param wxImage 要获取大图的图片消息
     */
    public void fetchImage(WXImage wxImage) {
        wxImage.origin = wxAPI.webwxgetmsgimg(wxImage.id, "big");
    }

    public void fetchVoice(WXVoice wxVoice) {
        wxVoice.voice = wxAPI.webwxgetvoice(wxVoice.id);
    }

    public void fetchVideo(WXVideo wxVideo) {
        wxVideo.video = wxAPI.webwxgetvideo(wxVideo.id);
    }

    /**
     * 获取文件消息的文件内容
     *
     * @param fileMsg 要获取文件内容的文件消息
     */
    public void fetchFile(WXFile fileMsg) {
        fileMsg.file = wxAPI.webwxgetmedia(fileMsg.id, fileMsg.fileName, fileMsg.fileId, fileMsg.fromUser.id);
    }

    /**
     * 同意好友申请
     *
     * @param userName     目标用户UserName
     * @param verifyTicket 验证票据
     */
    public void passVerify(String userName, String verifyTicket) {
        LOGGER.info(String.format("通过好友（%s）申请", userName));
        wxAPI.webwxverifyuser(3, userName, verifyTicket, "");
    }

    /**
     * 修改用户备注名
     *
     * @param userName 目标用户UserName
     * @param remark   备注名称
     */
    public void editRemark(String userName, String remark) {
        LOGGER.info(String.format("修改（%s）的备注：%s", userName, remark));
        wxAPI.webwxoplog(userName, remark);
    }

    /**
     * 添加聊天室的成员
     *
     * @param chatRoomName 聊天室的UserName
     * @param users        要添加的人员的UserName，必须是自己的好友
     */
    public void addGroupMember(String chatRoomName, List<String> users) {
        LOGGER.info(String.format("为群（%s）添加成员：%s", chatRoomName, users));
        wxAPI.webwxupdatechartroom(chatRoomName, "addmember", users);
    }

    /**
     * 移除聊天室的成员
     *
     * @param chatRoomName 聊天室的UserName
     * @param users        要移除的人员的UserName，必须是聊天室的成员，而且自己是管理员
     */
    public void delGroupMember(String chatRoomName, List<String> users) {
        LOGGER.info(String.format("为群（%s）删除成员：%s", chatRoomName, users));
        wxAPI.webwxupdatechartroom(chatRoomName, "delmember", users);
    }

    /**
     * 模拟网页微信客户端监听器
     */
    public static abstract class WeChatListener {
        /**
         * 获取到用户登录的二维码
         *
         * @param qrCode 用户登录二维码的url
         */
        public abstract void onQRCode(String qrCode);

        /**
         * 获取用户头像，base64编码
         *
         * @param base64Avatar base64编码的用户头像
         */
        public void onAvatar(String base64Avatar) {
        }

        /**
         * 模拟网页微信客户端异常退出
         *
         * @param reason 错误原因
         */
        public void onFailure(String reason) {
        }

        /**
         * 用户登录并初始化成功
         */
        public void onLogin() {
        }

        /**
         * 用户获取到消息
         */
        public void onMessage(WXMessage message) {
        }

        /**
         * 模拟网页微信客户端正常退出
         */
        public void onLogout() {
        }
    }

    /**
     * 模拟网页微信客户端工作线程
     */
    private class WeChatThread extends Thread {

        @Override
        public void run() {
            int loginCount = 0;
            while (!isInterrupted()) {
                //用户登录
                LOGGER.fine("正在登录");
                String loginErr = login();
                if (!XTools.strEmpty(loginErr)) {
                    LOGGER.severe(String.format("登录出现错误：%s", loginErr));
                    wxListener.onFailure(loginErr);
                    return;
                }
                //用户初始化
                LOGGER.fine("正在初始化");
                String initErr = initial();
                if (!XTools.strEmpty(initErr)) {
                    LOGGER.severe(String.format("初始化出现错误：%s", initErr));
                    wxListener.onFailure(initErr);
                    return;
                }
                wxListener.onLogin();
                //同步消息
                LOGGER.fine("正在监听消息");
                String listenErr = listen();
                if (!XTools.strEmpty(listenErr)) {
                    if (loginCount++ > 10) {
                        wxListener.onFailure(listenErr);
                        return;
                    } else {
                        continue;
                    }
                }
                //退出登录
                LOGGER.finer("正在退出登录");
                wxListener.onLogout();
                return;
            }
        }

        /**
         * 用户登录
         *
         * @return 登录时异常原因，为null表示正常登录
         */
        private String login() {
            try {
                if (XTools.strEmpty(wxAPI.sid)) {
                    String qrCode = wxAPI.jslogin();
                    LOGGER.finer(String.format("等待扫描二维码：%s", qrCode));
                    wxListener.onQRCode(qrCode);
                    while (true) {
                        RspLogin rspLogin = wxAPI.login();
                        switch (rspLogin.code) {
                            case 200:
                                LOGGER.finer("已授权登录");
                                wxAPI.webwxnewloginpage(rspLogin.redirectUri);
                                return null;
                            case 201:
                                LOGGER.finer("已扫描二维码");
                                wxListener.onAvatar(rspLogin.userAvatar);
                                break;
                            case 408:
                                LOGGER.finer("等待授权登录");
                                break;
                            default:
                                LOGGER.warning("登录超时");
                                return LOGIN_TIMEOUT;
                        }
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warning(String.format("登录异常：%s", Arrays.toString(e.getStackTrace())));
                return LOGIN_EXCEPTION;
            }
        }

        /**
         * 初始化
         *
         * @return 初始化异常原因，为null表示正常初始化
         */
        private String initial() {
            try {
                LOGGER.finer("正在获取Cookie");
                for (HttpCookie cookie : wxAPI.httpOption.cookieManager.getCookieStore().getCookies()) {
                    if ("wxsid".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.sid = cookie.getValue();
                    } else if ("wxuin".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.uin = cookie.getValue();
                    } else if ("webwx_data_ticket".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.dataTicket = cookie.getValue();
                    }
                }

                //获取自身信息
                LOGGER.finer("正在获取自身信息");
                RspInit rspInit = wxAPI.webwxinit();
                wxContacts.setMe(wxAPI.host, rspInit.User);

                //添加初始联系人，特殊账号等
                LOGGER.finer("正在添加初始联系人，特殊账号等");
                for (RspInit.User user : rspInit.ContactList) {
                    wxContacts.putContact(wxAPI.host, user);
                }

                //发送状态信息
                wxAPI.webwxstatusnotify(wxContacts.getMe().id, WXNotify.NOTIFY_INITED);

                //获取好友、群、公众号列表
                LOGGER.finer("正在获取好友、群、公众号列表");
                RspGetContact rspGetContact = wxAPI.webwxgetcontact();
                for (RspInit.User user : rspGetContact.MemberList) {
                    wxContacts.putContact(wxAPI.host, user);
                }

                //获取最近联系人
                LOGGER.finer("正在获取最近联系人");
                LinkedList<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
                if (!XTools.strEmpty(rspInit.ChatSet)) {
                    for (String userName : rspInit.ChatSet.split(",")) {
                        contacts.add(new ReqBatchGetContact.Contact(userName, ""));
                    }
                }
                loadContacts(contacts);

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warning(String.format("初始化异常：%s", e.getMessage()));
                return INIT_EXCEPTION;
            }
        }

        /**
         * 循环同步消息
         *
         * @return 同步消息的异常原因，为null表示正常结束
         */
        private String listen() {
            int retryCount = 0;
            try {
                while (!isInterrupted()) {
                    RspSyncCheck rspSyncCheck;
                    try {
                        LOGGER.finer("正在监听信息");
                        rspSyncCheck = wxAPI.synccheck();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (retryCount++ < 5) {
                            LOGGER.warning(String.format("监听异常，重试第%d次：\n%s", retryCount, Arrays.toString(e.getStackTrace())));
                            continue;
                        } else {
                            LOGGER.severe(String.format("监听异常，重试次数过多：\n%s", Arrays.toString(e.getStackTrace())));
                            return LISTEN_EXCEPTION;
                        }
                    }
                    retryCount = 0;
                    if (rspSyncCheck.retcode > 0) {
                        LOGGER.finer(String.format("停止监听信息：%d", rspSyncCheck.retcode));
                        return null;
                    } else if (rspSyncCheck.selector > 0) {
                        RspSync rspSync = wxAPI.webwxsync();
                        if (rspSync.DelContactList != null) {
                            //删除好友，删除群后的任意一条消息
                            for (RspInit.User user : rspSync.DelContactList) {
                                LOGGER.finer(String.format("删除联系人（%s）", user.UserName));
                                wxContacts.rmvContact(user.UserName);
                            }
                        }
                        if (rspSync.ModContactList != null) {
                            //被拉入群第一条消息，群里有人加入,群里踢人之后第一条信息，添加好友
                            for (RspInit.User user : rspSync.ModContactList) {
                                LOGGER.finer(String.format("变更联系人（%s）", user.UserName));
                                wxContacts.putContact(wxAPI.host, user);
                            }
                        }
                        if (rspSync.ModChatRoomMemberList != null) {
                            for (RspInit.User user : rspSync.ModChatRoomMemberList) {
                                LOGGER.finer(String.format("变更群成员（%s）", user.UserName));
                                wxContacts.putContact(wxAPI.host, user);
                            }
                        }
                        if (rspSync.AddMsgList != null) {
                            for (RspSync.AddMsg addMsg : rspSync.AddMsgList) {
                                wxListener.onMessage(parseMessage(addMsg));
                            }
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warning(String.format("监听消息异常：\n%s", Arrays.toString(e.getStackTrace())));
                return LISTEN_EXCEPTION;
            }
        }

        /**
         * 获取联系人信息
         *
         * @param contacts 要获取的联系人的列表
         */
        private void loadContacts(List<ReqBatchGetContact.Contact> contacts) {
            if (contacts.size() > 50) {
                LinkedList<ReqBatchGetContact.Contact> temp = new LinkedList<>();
                for (ReqBatchGetContact.Contact contact : contacts) {
                    temp.add(contact);
                    if (temp.size() >= 50) {
                        RspBatchGetContact rspBatchGetContact = wxAPI.webwxbatchgetcontact(contacts);
                        for (RspInit.User user : rspBatchGetContact.ContactList) {
                            wxContacts.putContact(wxAPI.host, user);
                        }
                        temp.clear();
                    }
                }
                contacts = temp;
            }
            if (contacts.size() > 0) {
                RspBatchGetContact rspBatchGetContact = wxAPI.webwxbatchgetcontact(contacts);
                for (RspInit.User user : rspBatchGetContact.ContactList) {
                    wxContacts.putContact(wxAPI.host, user);
                }
            }
        }

        private <T extends WXMessage> T parseCommon(RspSync.AddMsg msg, T message) {
            message.id = msg.MsgId;
            message.timestamp = msg.CreateTime * 1000;
            if (msg.FromUserName.startsWith("@@")) {
                if (wxContacts.getGroup(msg.FromUserName) == null) {
                    loadContacts(Collections.singletonList(new ReqBatchGetContact.Contact(msg.FromUserName, "")));
                }
                message.fromGroup = wxContacts.getGroup(msg.FromUserName);
                Matcher mGroupMsg = REX_GROUPMSG.matcher(msg.Content);
                if (mGroupMsg.matches()) {
                    if (wxContacts.getContact(mGroupMsg.group(1)) == null) {
                        if (message.fromGroup == null) {
                            message.fromUser = null;
                        } else {
                            List<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
                            for (Map.Entry<String, WXGroup.Member> entry : message.fromGroup.members.entrySet()) {
                                contacts.add(new ReqBatchGetContact.Contact(entry.getValue().id, ""));
                            }
                            loadContacts(contacts);
                            message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
                        }
                    }
                    message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
                }
                message.toContact = wxContacts.getContact(msg.ToUserName);
                if (message.fromUser != null) {
                    message.content = mGroupMsg.group(2);
                } else {
                    message.content = msg.Content;
                }
            } else {
                message.fromGroup = null;
                message.fromUser = (WXUser) wxContacts.getContact(msg.FromUserName);
                message.toContact = wxContacts.getContact(msg.ToUserName);
                message.content = msg.Content;
            }
            return message;
        }

        private WXMessage parseMessage(RspSync.AddMsg msg) {
            try {
                switch (msg.MsgType) {
                    case RspSync.AddMsg.TYPE_TEXT: {
                        if (msg.SubMsgType == 0) {
                            return parseCommon(msg, new WXText());
                        } else if (msg.SubMsgType == 48) {
                            WXLocation wxLocation = parseCommon(msg, new WXLocation());
                            wxLocation.locationName = wxLocation.content.substring(0, wxLocation.content.indexOf(':'));
                            wxLocation.locationImage = String.format("https://%s%s", wxAPI.host, wxLocation.content.substring(wxLocation.content.indexOf(':') + ":<br/>".length()));
                            wxLocation.locationUrl = msg.Url;
                            return wxLocation;
                        }
                        break;
                    }
                    case RspSync.AddMsg.TYPE_IMAGE: {
                        WXImage wxImage = parseCommon(msg, new WXImage());
                        wxImage.imgWidth = msg.ImgWidth;
                        wxImage.imgHeight = msg.ImgHeight;
                        wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "slave");
                        return wxImage;
                    }
                    case RspSync.AddMsg.TYPE_VOICE: {
                        WXVoice wxVoice = parseCommon(msg, new WXVoice());
                        wxVoice.voiceLength = msg.VoiceLength;
                        return wxVoice;
                    }
                    case RspSync.AddMsg.TYPE_VERIFY: {
                        WXVerify wxVerify = parseCommon(msg, new WXVerify());
                        wxVerify.userId = msg.RecommendInfo.UserName;
                        wxVerify.userName = msg.RecommendInfo.NickName;
                        wxVerify.signature = msg.RecommendInfo.Signature;
                        wxVerify.province = msg.RecommendInfo.Province;
                        wxVerify.city = msg.RecommendInfo.City;
                        wxVerify.gender = msg.RecommendInfo.Sex;
                        wxVerify.verify = msg.RecommendInfo.VerifyFlag;
                        wxVerify.ticket = msg.RecommendInfo.Ticket;
                        return wxVerify;
                    }
                    case RspSync.AddMsg.TYPE_RECOMMEND: {
                        WXRecommend wxRecommend = parseCommon(msg, new WXRecommend());
                        wxRecommend.userId = msg.RecommendInfo.UserName;
                        wxRecommend.userName = msg.RecommendInfo.NickName;
                        wxRecommend.gender = msg.RecommendInfo.Sex;
                        wxRecommend.signature = msg.RecommendInfo.Signature;
                        wxRecommend.province = msg.RecommendInfo.Province;
                        wxRecommend.city = msg.RecommendInfo.City;
                        wxRecommend.verifyFlag = msg.RecommendInfo.VerifyFlag;
                        return wxRecommend;
                    }
                    case RspSync.AddMsg.TYPE_VIDEO: {
                        //视频貌似可以分片，后期测试
                        WXVideo wxVideo = parseCommon(msg, new WXVideo());
                        wxVideo.imgWidth = msg.ImgWidth;
                        wxVideo.imgHeight = msg.ImgHeight;
                        wxVideo.videoLength = msg.PlayLength;
                        wxVideo.image = wxAPI.webwxgetmsgimg(msg.MsgId, "slave");
                        return wxVideo;
                    }
                    case RspSync.AddMsg.TYPE_EMOJI: {
                        WXImage wxImage = parseCommon(msg, new WXImage());
                        wxImage.imgWidth = msg.ImgWidth;
                        wxImage.imgHeight = msg.ImgHeight;
                        wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
                        wxImage.origin = wxImage.image;
                        return wxImage;
                    }
                    case RspSync.AddMsg.TYPE_OTHER: {
                        if (msg.AppMsgType == 2) {
                            WXImage wxImage = parseCommon(msg, new WXImage());
                            wxImage.imgWidth = msg.ImgWidth;
                            wxImage.imgHeight = msg.ImgHeight;
                            wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
                            wxImage.origin = wxImage.image;
                            return wxImage;
                        } else if (msg.AppMsgType == 5) {
                            WXLink wxLink = parseCommon(msg, new WXLink());
                            wxLink.linkName = msg.FileName;
                            wxLink.linkUrl = msg.Url;
                            return wxLink;
                        } else if (msg.AppMsgType == 6) {
                            WXFile wxFile = parseCommon(msg, new WXFile());
                            wxFile.fileId = msg.MediaId;
                            wxFile.fileName = msg.FileName;
                            wxFile.fileSize = XTools.strEmpty(msg.FileSize) ? 0 : Long.valueOf(msg.FileSize);
                            return wxFile;
                        } else if (msg.AppMsgType == 8) {
                            WXImage wxImage = parseCommon(msg, new WXImage());
                            wxImage.imgWidth = msg.ImgWidth;
                            wxImage.imgHeight = msg.ImgHeight;
                            wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
                            wxImage.origin = wxImage.image;
                            return wxImage;
                        } else if (msg.AppMsgType == 2000) {
                            return parseCommon(msg, new WXMoney());
                        }
                        break;
                    }
                    case RspSync.AddMsg.TYPE_NOTIFY: {
                        WXNotify wxNotify = parseCommon(msg, new WXNotify());
                        wxNotify.notifyCode = msg.StatusNotifyCode;
                        wxNotify.notifyContact = msg.StatusNotifyUserName;
                        return wxNotify;
                    }
                    case RspSync.AddMsg.TYPE_SYSTEM: {
                        return parseCommon(msg, new WXSystem());
                    }
                    case RspSync.AddMsg.TYPE_REVOKE:
                        WXRevoke wxRevoke = parseCommon(msg, new WXRevoke());
                        Matcher idMatcher = REX_REVOKE_ID.matcher(wxRevoke.content);
                        if (idMatcher.find()) {
                            wxRevoke.msgId = Long.valueOf(idMatcher.group(1));
                        }
                        Matcher replaceMatcher = REX_REVOKE_REPLACE.matcher(wxRevoke.content);
                        if (replaceMatcher.find()) {
                            wxRevoke.msgReplace = replaceMatcher.group(1);
                        }
                        return wxRevoke;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.warning("消息解析失败");
            }
            return parseCommon(msg, new WXUnknown());
        }
    }
}
