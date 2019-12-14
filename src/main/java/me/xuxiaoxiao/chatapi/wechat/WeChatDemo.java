package me.xuxiaoxiao.chatapi.wechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXContact;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXGroup;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXMessage;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXUnknown;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXVerify;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Scanner;

public class WeChatDemo {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * 新建一个微信监听器
     */
    public static final WeChatClient.WeChatListener LISTENER = new WeChatClient.WeChatListener() {
        @Override
        public void onQRCode(@Nonnull WeChatClient client, @Nonnull String qrCode) {
            System.out.println("onQRCode：" + qrCode);
        }

        @Override
        public void onLogin(@Nonnull WeChatClient client) {
            System.out.println(String.format("onLogin：您有%d名好友、活跃微信群%d个", client.userFriends().size(), client.userGroups().size()));
        }

        @Override
        public void onMessage(@Nonnull WeChatClient client, @Nonnull WXMessage message) {
            System.out.println("获取到消息：" + GSON.toJson(message));

//            if (message instanceof WXVerify) {
//                //是好友请求消息，自动同意好友申请
//                client.passVerify((WXVerify) message);
//            } else if (message instanceof WXLocation && message.fromUser != null && !message.fromUser.id.equals(client.userMe().id)) {
//                // 如果对方告诉我他的位置，发送消息的不是自己，则我也告诉他我的位置
//                if (message.fromGroup != null) {
//                    // 群消息
//                    // client.sendLocation(message.fromGroup, "120.14556", "30.23856", "我在这里", "西湖");
//                } else {
//                    // 用户消息
//                    client.sendLocation(message.fromUser, "120.14556", "30.23856", "我在这里", "西湖");
//                }
//            } else if (message instanceof WXText && message.fromUser != null && !message.fromUser.id.equals(client.userMe().id)) {
//                //是文字消息，并且发送消息的人不是自己，发送相同内容的消息
//                if (message.fromGroup != null) {
//                    // 群消息
//                    // client.sendText(message.fromGroup, message.content);
//                } else {
//                    // 用户消息
//                    client.sendText(message.fromUser, message.content);
//                }
//            }
        }

        @Override
        public void onContact(@Nonnull WeChatClient client, @Nullable WXContact oldContact, @Nullable WXContact newContact) {
            System.out.println(String.format("检测到联系人变更:旧联系人名称：%s:新联系人名称：%s", (oldContact == null ? "null" : oldContact.name), (newContact == null ? "null" : newContact.name)));
        }
    };


    public static void main(String[] args) {
        //新建一个模拟微信客户端
        WeChatClient wechatClient = new WeChatClient();
        //为模拟微信客户端设置监听器
        wechatClient.setListener(LISTENER);
        //启动模拟微信客户端
        wechatClient.startup();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("请输入指令");
                switch (scanner.nextLine()) {
                    case "sendText": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("textContent:");
                        String text = scanner.nextLine();
                        WXContact contact = wechatClient.userContact(toContactId);
                        if (contact != null) {
                            System.out.println("success:" + GSON.toJson(wechatClient.sendText(contact, text)));
                        } else {
                            System.out.println("联系人未找到");
                        }
                    }
                    break;
                    case "sendFile": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("filePath:");
                        File file = new File(scanner.nextLine());
                        WXContact contact = wechatClient.userContact(toContactId);
                        if (contact != null) {
                            System.out.println("success:" + GSON.toJson(wechatClient.sendFile(contact, file)));
                        } else {
                            System.out.println("联系人未找到");
                        }
                    }
                    break;
                    case "sendLocation": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("longitude:");
                        String longitude = scanner.nextLine();
                        System.out.println("latitude:");
                        String latitude = scanner.nextLine();
                        System.out.println("title:");
                        String title = scanner.nextLine();
                        System.out.println("lable:");
                        String lable = scanner.nextLine();
                        WXContact contact = wechatClient.userContact(toContactId);
                        if (contact != null) {
                            System.out.println("success:" + GSON.toJson(wechatClient.sendLocation(contact, longitude, latitude, title, lable)));
                        } else {
                            System.out.println("联系人未找到");
                        }
                    }
                    break;
                    case "revokeMsg": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("clientMsgId:");
                        String clientMsgId = scanner.nextLine();
                        System.out.println("serverMsgId:");
                        String serverMsgId = scanner.nextLine();
                        WXUnknown wxUnknown = new WXUnknown();
                        wxUnknown.id = Long.valueOf(serverMsgId);
                        wxUnknown.idLocal = Long.valueOf(clientMsgId);
                        wxUnknown.toContact = wechatClient.userContact(toContactId);
                        wechatClient.revokeMsg(wxUnknown);
                    }
                    break;
                    case "passVerify": {
                        System.out.println("userId:");
                        String userId = scanner.nextLine();
                        System.out.println("verifyTicket:");
                        String verifyTicket = scanner.nextLine();
                        WXVerify wxVerify = new WXVerify();
                        wxVerify.userId = userId;
                        wxVerify.ticket = verifyTicket;
                        wechatClient.passVerify(wxVerify);
                    }
                    break;
                    case "editRemark": {
                        System.out.println("userId:");
                        String userId = scanner.nextLine();
                        System.out.println("remarkName:");
                        String remark = scanner.nextLine();
                        WXContact contact = wechatClient.userContact(userId);
                        if (contact instanceof WXUser) {
                            wechatClient.editRemark((WXUser) contact, remark);
                        } else {
                            System.out.println("好友未找到");
                        }
                    }
                    break;
                    case "topContact": {
                        System.out.println("contactId:");
                        String contactId = scanner.nextLine();
                        System.out.println("isTop:");
                        String isTop = scanner.nextLine();
                        WXContact contact = wechatClient.userContact(contactId);
                        if (contact != null) {
                            wechatClient.topContact(contact, Boolean.valueOf(isTop.toLowerCase()));
                        } else {
                            System.out.println("联系人未找到");
                        }
                    }
                    break;
                    case "setGroupName": {
                        System.out.println("groupId:");
                        String groupId = scanner.nextLine();
                        System.out.println("name:");
                        String name = scanner.nextLine();
                        WXGroup group = wechatClient.userGroup(groupId);
                        if (group != null) {
                            wechatClient.setGroupName(group, name);
                        } else {
                            System.out.println("群组未找到");
                        }
                    }
                    break;
                    case "quit": {
                        System.out.println("logging out");
                        wechatClient.shutdown();
                    }
                    return;
                    default: {
                        System.out.println("未知指令");
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}