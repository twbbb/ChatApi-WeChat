# ChatApi-WeChat
Java版本微信聊天接口，使用网页微信API，让你能够开发自己的微信聊天机器人。

Java版本QQ聊天接口请看[ChatApi-QQ](https://github.com/xuxiaoxiao-xxx/ChatApi-QQ)

## 这是什么？
* 这是一个模拟的微信聊天客户端
* 该客户端使用的接口来自于网页版微信

## 有何优点？
* 对接口、流程、实体类进行了封装，更加简单易用
* 暴露了一个监听器，可以自己实现监听器以开发自己的业务功能
* 全部功能的支持
    * 监听文字、图像、动图、语音、视频、文件、系统提示等消息
    * 发送文字、图像、动图、文件等消息
    * 撤回发送的消息
    * 同意好友申请（网页微信发送好友申请功能已被关闭）
    * 修改好友备注
    * 添加和移除群成员（网页微信创建群功能已被关闭）

## 测试数据
* 最后测试可用时间：2018-06-10
* 最长在线时间：7天

## 如何使用
* maven依赖

```xml
<dependency>
    <groupId>me.xuxiaoxiao</groupId>
    <artifactId>chatapi-wechat</artifactId>
    <version>1.1.1</version>
</dependency>
```

* gradle依赖

```gradle
implementation 'me.xuxiaoxiao:chatapi-wechat:1.1.1'
```

* jar包

[点击进入下载页](https://github.com/xuxiaoxiao-xxx/ChatApi-WeChat/releases)

* 以下是一个学别人说话的小机器人，用到了该库提供的大部分功能
```java
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
```