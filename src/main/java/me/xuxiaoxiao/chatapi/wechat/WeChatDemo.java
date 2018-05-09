package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.chatapi.wechat.protocol.RspInit;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class WeChatDemo {
    /**
     * 新建一个模拟微信客户端，并绑定一个简单的监听器
     */
    public static WeChatClient wechatClient = new WeChatClient(new WeChatClient.WeChatListener() {
        @Override
        public void onQRCode(String qrCode) {
            System.out.println("onQRCode:" + qrCode);
        }

        @Override
        public void onLogin() {
            System.out.println("onLogin");
            System.out.println(String.format("您有%d名好友、关注%d个公众号、活跃微信群%d个", wechatClient.userFriends().size(), wechatClient.userPublics().size(), wechatClient.userChatrooms().size()));
        }

        @Override
        public void onMessageText(String msgId, RspInit.User userWhere, RspInit.User userFrom, String text) {
            System.out.println("onMessageText:" + text);
            //学习别人说话
            if (!userFrom.UserName.equals(wechatClient.userMe().UserName)) {
                wechatClient.sendText(userWhere.UserName, text);
            }
        }
    });

    public static void main(String[] args) {
        //启动模拟微信客户端
        wechatClient.startup();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("请输入指令");
            switch (scanner.nextLine()) {
                case "sendText": {
                    System.out.println("toUserName:");
                    String toUserName = scanner.nextLine();
                    System.out.println("textContent:");
                    String text = scanner.nextLine();
                    wechatClient.sendText(toUserName, text);
                }
                break;
                case "sendImage": {
                    try {
                        System.out.println("toUserName:");
                        String toUserName = scanner.nextLine();
                        System.out.println("imagePath:");
                        File image = new File(scanner.nextLine());
                        wechatClient.sendImage(toUserName, image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
                case "revokeMsg": {
                    System.out.println("toUserName:");
                    String toUserName = scanner.nextLine();
                    System.out.println("clientMsgId:");
                    String clientMsgId = scanner.nextLine();
                    System.out.println("serverMsgId:");
                    String serverMsgId = scanner.nextLine();
                    wechatClient.revokeMsg(toUserName, clientMsgId, serverMsgId);
                }
                break;
                case "sendVerify": {
                    System.out.println("userName:");
                    String userName = scanner.nextLine();
                    System.out.println("verifyContent:");
                    String verifyContent = scanner.nextLine();
                    wechatClient.sendVerify(userName, verifyContent);
                }
                break;
                case "passVerify": {
                    System.out.println("userName:");
                    String userName = scanner.nextLine();
                    System.out.println("verifyTicket:");
                    String verifyTicket = scanner.nextLine();
                    wechatClient.passVerify(userName, verifyTicket);
                }
                break;
                case "editRemark": {
                    System.out.println("userName:");
                    String userName = scanner.nextLine();
                    System.out.println("remarkName:");
                    String remark = scanner.nextLine();
                    wechatClient.editRemark(userName, remark);
                }
                break;
                case "createChatroom": {
                    System.out.println("topic:");
                    String topic = scanner.nextLine();
                    System.out.println("members,split by ',':");
                    String members = scanner.nextLine();
                    String chatroomName = wechatClient.createChatroom(topic, Arrays.asList(members.split(",")));
                    System.out.println("create chatroom " + chatroomName);
                }
                break;
                case "addChatroomMember": {
                    System.out.println("chatRoomName:");
                    String chatroomName = scanner.nextLine();
                    System.out.println("members,split by ',':");
                    String members = scanner.nextLine();
                    wechatClient.addChatroomMember(chatroomName, Arrays.asList(members.split(",")));
                }
                break;
                case "delChatroomMember": {
                    System.out.println("chatRoomName:");
                    String chatroomName = scanner.nextLine();
                    System.out.println("members,split by ',':");
                    String members = scanner.nextLine();
                    wechatClient.delChatroomMember(chatroomName, Arrays.asList(members.split(",")));
                }
                break;
                case "quit":
                    System.out.println("logging out");
                    wechatClient.shutdown();
                    return;
                default:
                    System.out.println("未知指令");
                    break;
            }
        }
    }
}