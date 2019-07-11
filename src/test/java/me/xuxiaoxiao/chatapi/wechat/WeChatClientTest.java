package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXGroup;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;
import me.xuxiaoxiao.xtools.common.XTools;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;

public class WeChatClientTest {
    public static final String TEST_TARGET_USER = "XXX";
    public static final String TEST_TARGET_GROUP = "XXX";

    private static final WeChatClient CLIENT = new WeChatClient();
    private static final WeChatClient.WeChatListener LISTENER = new WeChatClient.WeChatListener() {
        @Override
        public void onQRCode(@Nonnull WeChatClient client, @Nonnull String qrCode) {
            XTools.logD(WeChatClient.LOG_TAG, "请扫描二维码以继续测试：%s", qrCode);
        }

        @Override
        public void onLogin(@Nonnull WeChatClient client) {
            for (WXUser user : client.userFriends().values()) {
                if (user.name.equals(TEST_TARGET_USER)) {
                    TARGET_USER = user;
                    break;
                }
            }
            for (WXGroup group : client.userGroups().values()) {
                if (group.name.equals(TEST_TARGET_GROUP)) {
                    TARGET_GROUP = group;
                    break;
                }
            }
            synchronized (WeChatClientTest.CLIENT) {
                WeChatClientTest.CLIENT.notifyAll();
            }
        }
    };
    private static WXUser TARGET_USER = null;
    private static WXGroup TARGET_GROUP = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        CLIENT.setListener(LISTENER);
        CLIENT.startup();
        synchronized (CLIENT) {
            CLIENT.wait();
        }
    }

    @Test
    public void testSendText() {
        CLIENT.sendText(TARGET_USER, "测试向好友发送消息");
        pause(1000);
        CLIENT.sendText(TARGET_GROUP, "测试向群发送消息");
        pause(1000);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        CLIENT.shutdown();
    }

    private void pause(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}