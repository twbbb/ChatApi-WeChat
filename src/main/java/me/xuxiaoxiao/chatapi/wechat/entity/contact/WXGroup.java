package me.xuxiaoxiao.chatapi.wechat.entity.contact;

import java.io.Serializable;
import java.util.HashMap;

public class WXGroup extends WXContact implements Serializable, Cloneable {
    /**
     * 我自己是否是群主
     */
    public boolean isOwner;
    /**
     * 群成员id到entity的映射
     */
    public HashMap<String, Member> members;

    public static class Member implements Serializable, Cloneable {
        /**
         * 群成员id
         */
        public String id;
        /**
         * 群成员昵称
         */
        public String name;
        /**
         * 群成员群名片
         */
        public String display;
    }
}
