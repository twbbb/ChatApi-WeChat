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
    * 置顶/取消置顶联系人
    * 设置群名称
    * （网页微信创建群、添加群成员、移除群成员功能均已被关闭）

## 测试数据
* 最后测试可用时间：2018-10-14
* 最长在线时间：7天

## 如何使用
* maven依赖

```xml
<dependency>
    <groupId>me.xuxiaoxiao</groupId>
    <artifactId>chatapi-wechat</artifactId>
    <version>1.1.5</version>
</dependency>
```

* gradle依赖

```gradle
implementation 'me.xuxiaoxiao:chatapi-wechat:1.1.5'
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
    public static WeChatClient WECHAT_CLIENT = new WeChatClient(new WeChatClient.WeChatListener() {
        @Override
        public void onQRCode(String qrCode) {
            System.out.println("onQRCode：" + qrCode);
        }
        
        @Override
        public void onLogin() {
            System.out.println(String.format("onLogin：您有%d名好友、活跃微信群%d个", WECHAT_CLIENT.userFriends().size(), WECHAT_CLIENT.userGroups().size()));
        }
        
        @Override
        public void onMessage(WXMessage message) {
            System.out.println("获取到消息：" + GSON.toJson(message));
            
            if (message instanceof WXVerify) {
                //是好友请求消息，自动同意好友申请
                WECHAT_CLIENT.passVerify((WXVerify) message);
            } else if (message instanceof WXText && message.fromUser != null && !message.fromUser.id.equals(WECHAT_CLIENT.userMe().id)) {
                //是文字消息，并且发送消息的人不是自己，发送相同内容的消息
                if (message.fromGroup != null) {
                    //群消息
                    WECHAT_CLIENT.sendText(message.fromGroup, message.content);
                } else {
                    //用户消息
                    WECHAT_CLIENT.sendText(message.fromUser, message.content);
                }
            }
        }
        
        @Override
        public void onContact(WXContact contact, int operate) {
            System.out.println(String.format("检测到联系人变更:%s:%s", operate == WeChatClient.ADD_CONTACT ? "新增" : (operate == WeChatClient.DEL_CONTACT ? "删除" : "修改"), contact.name));
        }
    });
    
    public static void main(String[] args) {
        //启动模拟微信客户端
        WECHAT_CLIENT.startup();
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
                        System.out.println("success:" + GSON.toJson(WECHAT_CLIENT.sendText(WECHAT_CLIENT.userContact(toContactId), text)));
                    }
                    break;
                    case "sendFile": {
                        System.out.println("toContactId:");
                        String toContactId = scanner.nextLine();
                        System.out.println("filePath:");
                        File file = new File(scanner.nextLine());
                        System.out.println("success:" + GSON.toJson(WECHAT_CLIENT.sendFile(WECHAT_CLIENT.userContact(toContactId), file)));
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
                        wxUnknown.toContact = WECHAT_CLIENT.userContact(toContactId);
                        WECHAT_CLIENT.revokeMsg(wxUnknown);
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
                        WECHAT_CLIENT.passVerify(wxVerify);
                    }
                    break;
                    case "editRemark": {
                        System.out.println("userId:");
                        String userId = scanner.nextLine();
                        System.out.println("remarkName:");
                        String remark = scanner.nextLine();
                        WECHAT_CLIENT.editRemark((WXUser) WECHAT_CLIENT.userContact(userId), remark);
                    }
                    break;
                    case "topContact": {
                        System.out.println("contactId:");
                        String contactId = scanner.nextLine();
                        System.out.println("isTop:");
                        String isTop = scanner.nextLine();
                        WECHAT_CLIENT.topContact(WECHAT_CLIENT.userContact(contactId), Boolean.valueOf(isTop.toLowerCase()));
                    }
                    break;
                    case "setGroupName": {
                        System.out.println("groupId:");
                        String groupId = scanner.nextLine();
                        System.out.println("name:");
                        String name = scanner.nextLine();
                        WECHAT_CLIENT.setGroupName(WECHAT_CLIENT.userGroup(groupId), name);
                    }
                    break;
                    case "quit": {
                        System.out.println("logging out");
                        WECHAT_CLIENT.shutdown();
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
```