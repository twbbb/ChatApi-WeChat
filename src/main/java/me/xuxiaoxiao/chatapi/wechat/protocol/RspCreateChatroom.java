package me.xuxiaoxiao.chatapi.wechat.protocol;

import java.util.ArrayList;

public class RspCreateChatroom {
    public BaseResponse BaseResponse;
    public String Topic;
    public String PYInitial;
    public String QuanPin;
    public int MemberCount;
    public ArrayList<RspInit.User> MemberList;
    public String ChatRoomName;
    public String BlackList;
}
