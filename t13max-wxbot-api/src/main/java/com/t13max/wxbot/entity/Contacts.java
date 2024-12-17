package com.t13max.wxbot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * 联系人信息
 *
 * @Author t13max
 * @Date 14:36 2024/12/16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contacts {
    //用户名
    private String username;
    //昵称
    private String nickname;
    //签名
    private String signature;
    //性别
    private Byte sex;
    //备注名
    private String remarkname;
    //省份
    private String province;
    //城市
    private String city;
    //chatroomid
    private Double chatroomid;
    //attrstatus
    private Double attrstatus;
    // 0消息免打扰 1正常
    private Double statues;

    private String pyquanpin;

    private String encrychatroomid;

    private String displayname;

    private Integer verifyflag;

    private Double unifriend;

    private Double contactflag;

    private List<Contacts> memberlist;

    private Double starfriend;

    private String headimgurl;

    private Double appaccountflag;

    private Double membercount;

    private String remarkpyinitial;


    private Double snsflag;

    private String alias;

    private String keyword;

    private Double hideinputbarflag;

    private String remarkpyquanpin;

    private Double uin;

    private Double owneruin;

    private Double isowner;

    private String pyinitial;

    private String ticket;


    /**
     * 联系人类型
     */
    public enum ContactsType {
        GROUP_USER((byte) 1, "群组"), PUBLIC_USER((byte) 2, "公众号"), SPECIAL_USER((byte) 3, "特殊账号"), ORDINARY_USER((byte) 4, "普通用户");
        public final byte code;
        public final String desc;

        ContactsType(byte code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    private ContactsType type = ContactsType.ORDINARY_USER;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contacts contacts = (Contacts) o;
        return contacts.username.equals(username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}