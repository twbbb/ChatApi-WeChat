package me.xuxiaoxiao.chatapi.wechat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXMessage;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXText;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXUnknown;
import me.xuxiaoxiao.chatapi.wechat.entity.message.WXVerify;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class WeChatDemo {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    /**
     * 新建一个模拟微信客户端，并绑定一个简单的监听器
     */
    public static WeChatClient wechatClient = new WeChatClient(new WeChatClient.WeChatListener() {
        @Override
        public void onQRCode(String qrCode) {
            System.out.println("onQRCode：" + qrCode);
        }

        @Override
        public void onLogin() {
            System.out.println(String.format("onLogin：您有%d名好友、活跃微信群%d个", wechatClient.userFriends().size(), wechatClient.userGroups().size()));
        }

        @Override
        public void onMessage(WXMessage message) {
            System.out.println("获取到消息：" + GSON.toJson(message));
            if (message instanceof WXText && message.fromUser != null && !message.fromUser.id.equals(wechatClient.userMe().id)) {
                //是文字消息，并且发送消息的人不是自己，发送相同内容的消息
                if (message.fromGroup != null) {
                    //群消息
                    wechatClient.sendText(message.fromGroup, message.content);
                } else {
                    //用户消息
                    wechatClient.sendText(message.fromUser, message.content);
                }
            }
        }
    });

    public static void main(String[] args) {
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
                        System.out.println("success:" + GSON.toJson(wechatClient.sendText(wechatClient.userContact(toContactId), text)));
                    }
                    break;
                    case "sendFile": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("filePath:");
                        File file = new File(scanner.nextLine());
                        System.out.println("success:" + GSON.toJson(wechatClient.sendFile(wechatClient.userContact(toContactId), file)));
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
                        wechatClient.editRemark((WXUser) wechatClient.userContact(userId), remark);
                    }
                    break;
                    case "addGroupMember": {
                        System.out.println("groupId:");
                        String groupId = scanner.nextLine();
                        System.out.println("memberIds,split by ',':");
                        String memberIds = scanner.nextLine();
                        wechatClient.addGroupMember(wechatClient.userGroup(groupId), Arrays.asList(memberIds.split(",")));
                    }
                    break;
                    case "delGroupMember": {
                        System.out.println("chatRoomName:");
                        String groupId = scanner.nextLine();
                        System.out.println("memberIds,split by ',':");
                        String memberIds = scanner.nextLine();
                        wechatClient.delGroupMember(wechatClient.userGroup(groupId), Arrays.asList(memberIds.split(",")));
                    }
                    break;
                    case "quit":
                        System.out.println("logging out");
                        wechatClient.shutdown();
                        return;
                    default:
                        System.out.println("未知指令");
                        return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}