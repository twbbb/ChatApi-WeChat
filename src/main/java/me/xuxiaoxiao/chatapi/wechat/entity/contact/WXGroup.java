package me.xuxiaoxiao.chatapi.wechat.entity.contact;

import java.io.Serializable;
import java.util.HashMap;

/**
 * 微信群
 */
public class WXGroup extends WXContact implements Serializable, Cloneable {
    /**
     * 我自己是否是群主
     */
    public boolean isOwner;
    /**
     * 群成员id到entity的映射
     */
    public HashMap<String, Member> members;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        WXGroup wxGroup = (WXGroup) o;
        if (isOwner != wxGroup.isOwner) {
            return false;
        }
        return members != null ? members.equals(wxGroup.members) : wxGroup.members == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (isOwner ? 1 : 0);
        result = 31 * result + (members != null ? members.hashCode() : 0);
        return result;
    }

    @Override
    public WXGroup clone() {
        WXGroup wxGroup = (WXGroup) super.clone();
        if (this.members != null) {
            wxGroup.members = (HashMap<String, Member>) this.members.clone();
        }
        return wxGroup;
    }

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
