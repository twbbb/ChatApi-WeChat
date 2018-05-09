package me.xuxiaoxiao.chatapi.wechat.protocol;

public class ReqSendMsg {
    public BaseRequest BaseRequest;
    public Msg Msg;
    public int Scene;

    public ReqSendMsg(BaseRequest baseRequest, Msg msg) {
        this.BaseRequest = baseRequest;
        this.Msg = msg;
        this.Scene = 0;
    }

    public static class Msg {
        public int Type;
        public String MediaId;
        public String Content;
        public String FromUserName;
        public String ToUserName;
        public String LocalID;
        public String ClientMsgId;

        public Msg(int type, String mediaId, String content, String fromUserName, String toUserName) {
            this.Type = type;
            this.MediaId = mediaId;
            this.Content = content;
            this.FromUserName = fromUserName;
            this.ToUserName = toUserName;
            this.LocalID = msgId();
            this.ClientMsgId = LocalID;
        }

        public static String msgId() {
            StringBuilder sbRandom = new StringBuilder().append(System.currentTimeMillis());
            for (int i = 0; i < 4; i++) {
                sbRandom.append((int) (Math.random() * 10));
            }
            return sbRandom.toString();
        }
    }
}
