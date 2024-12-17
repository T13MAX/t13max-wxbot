package com.t13max.wxbot.tools;


import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.WxConstant;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.utils.CommonTools;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 微信联系人工具，如获好友昵称、备注等
 *
 * @Author t13max
 * @Date 15:59 2024/12/16
 */
@Log4j2
public class ContactsTools {

    /**
     * 根据用户名获取用户信息
     *
     * @param userName 用户UserName
     * @return 用户信息
     */
    public static Contacts getContactByUserName(Robot robot, String userName) {
        Map<String, Contacts> contactMap = robot.getMemberMap();
        return contactMap.getOrDefault(userName, null);

    }

    /**
     * 根据用户名获取用户显示名称
     * 有备注显示备注，无备注显示昵称
     * 群则直接显示昵称
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactDisplayNameByUserName(Robot robot, String userName) {
        String remarkNameByPersonUserName = getContactRemarkNameByUserName(robot, userName);
        if (StringUtils.isNotEmpty(remarkNameByPersonUserName)) {
            return remarkNameByPersonUserName;
        }
        String nickNameByPersonUserName = getContactNickNameByUserName(robot, userName);
        if (StringUtils.isNotEmpty(nickNameByPersonUserName)) {
            return nickNameByPersonUserName;
        }
        String groupDefaultName = getGroupDefaultName(robot, userName);
        if (StringUtils.isNotEmpty(groupDefaultName)) {
            return groupDefaultName;
        }
        return userName;
    }

    /**
     * 根据用户名获取用户显示名称
     * 有备注显示备注，无备注显示昵称
     * 群则直接显示昵称
     *
     * @param contacts 用户U
     * @return 备注
     */
    public static String getContactDisplayNameByUserName(Contacts contacts) {
        String remarkNameByPersonUserName = getContactRemarkNameByUserName(contacts);
        if (StringUtils.isNotEmpty(remarkNameByPersonUserName)) {
            return remarkNameByPersonUserName;
        }
        String nickNameByPersonUserName = getContactNickNameByUserName(contacts);
        if (StringUtils.isNotEmpty(nickNameByPersonUserName)) {
            return nickNameByPersonUserName;
        }
        String groupDefaultName = getGroupDefaultName(contacts);
        if (StringUtils.isNotEmpty(groupDefaultName)) {
            return groupDefaultName;
        }
        return contacts.getUsername();
    }

    /**
     * 获取群聊的默认名称
     *
     * @param userName 用户UserName
     * @return 默认名称，如果是群，则以群成员的名称开始
     */
    public static String getGroupDefaultName(Robot robot, String userName) {

        if (userName != null && userName.startsWith("@@")) {

            return Optional.ofNullable(getContactByUserName(robot, userName))
                    .map(Contacts::getMemberlist)
                    .map(memberList -> memberList.stream()
                            .map(Contacts::getNickname)
                            .limit(2)
                            .collect(Collectors.joining(",")))
                    .orElse(null);
        }
        return null;
    }

    /**
     * 获取群聊的默认名称
     *
     * @param contacts 用户UserName
     * @return 默认名称，如果是群，则以群成员的名称开始
     */
    public static String getGroupDefaultName(Contacts contacts) {

        if (contacts != null && contacts.getUsername().startsWith("@@")) {

            return Optional.of(contacts)
                    .map(Contacts::getMemberlist)
                    .map(memberList -> memberList.stream()
                            .map(Contacts::getNickname)
                            .limit(2)
                            .collect(Collectors.joining(",")))
                    .orElse(null);
        }
        return null;
    }

    /**
     * 根据用户名获取用户备注
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactRemarkNameByUserName(Robot robot, String userName) {
        if (userName == null) {
            return "";
        }
        //群只有备注 没有昵称
        if (userName.startsWith("@@")) {
            return getContactNickNameByUserName(robot, userName);
        }
        Contacts contactByUserName = getContactByUserName(robot, userName);
        if (contactByUserName == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contactByUserName.getRemarkname());
    }

    /**
     * 根据用户名获取用户备注
     *
     * @param contacts 用户UserName
     * @return 备注
     */
    public static String getContactRemarkNameByUserName(Contacts contacts) {
        if (contacts == null) {
            return null;
        }
        //群只有备注 没有昵称
        if (contacts.getUsername().startsWith("@@")) {
            return getContactNickNameByUserName(contacts);
        }
        return CommonTools.emojiFormatter(contacts.getRemarkname());
    }

    /**
     * 根据用户名获取普通用户昵称
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactNickNameByUserName(Robot robot, String userName) {
        if (userName == null) {
            return null;
        }
        Contacts contactByUserName = getContactByUserName(robot, userName);
        if (contactByUserName == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contactByUserName.getNickname());
    }

    /**
     * 根据用户名获取普通用户昵称
     *
     * @param contacts 用户UserName
     * @return 备注
     */
    public static String getContactNickNameByUserName(Contacts contacts) {
        if (contacts == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contacts.getNickname());
    }

    /**
     * 获取群成员
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 成员
     */
    public static Contacts getMemberOfGroup(Robot robot, String groupName, String userName) {
        if (StringUtils.isEmpty(userName)) {
            return null;
        }
        long l = System.currentTimeMillis();
        Optional<Contacts> contacts1 = Optional.ofNullable(groupName)
                .map(robot.getMemberMap()::get)
                .map(Contacts::getMemberlist)
                .flatMap(memberList -> memberList.stream()
                        .filter(contacts -> userName.equals(contacts.getUsername()))
                        .findAny());
        return contacts1.orElse(null);
    }

    /**
     * 获取群成员
     *
     * @param group    群
     * @param userName 成员UserName
     * @return 成员
     */
    public static Contacts getMemberOfGroup(Contacts group, String userName) {
        if (StringUtils.isEmpty(userName)) {
            return null;
        }

        Optional<Contacts> contacts1 = Optional.of(group)
                .map(Contacts::getMemberlist)
                .flatMap(memberList -> memberList.stream()
                        .filter(contacts -> userName.equals(contacts.getUsername()))
                        .findAny());
        return contacts1.orElse(null);
    }

    /**
     * 获取群成员昵称
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 成员昵称
     */
    public static String getMemberNickNameOfGroup(Robot robot, String groupName, String userName) {
        Contacts memberOfGroup = getMemberOfGroup(robot, groupName, userName);
        return memberOfGroup != null
                ? CommonTools.emojiFormatter(memberOfGroup.getNickname())
                : null;


    }

    /**
     * 获取群成员显示名称
     *
     * @param groupName 群UserName
     * @param userName  成员UserName
     * @return 群成员显示名称
     */
    public static String getMemberDisplayNameOfGroup(Robot robot, String groupName, String userName) {
        Contacts memberOfGroup = getMemberOfGroup(robot, groupName, userName);
        return getMemberDisplayNameOfGroup(memberOfGroup, userName);
    }

    /**
     * 获取群成员显示名称
     *
     * @param group    群
     * @param userName 成员UserName
     * @return 群成员显示名称
     */
    public static String getMemberDisplayNameOfGroupObj(Contacts group, String userName) {
        Contacts memberOfGroup = getMemberOfGroup(group, userName);
        return getMemberDisplayNameOfGroup(memberOfGroup, userName);
    }

    /**
     * 获取群成员显示名称
     *
     * @param memberOfGroup 群
     * @param userName      成员UserName
     * @return 群成员显示名称
     */
    public static String getMemberDisplayNameOfGroup(Contacts memberOfGroup, String userName) {
        if (memberOfGroup == null || userName == null) {
            return "";
        }
        String displayName = memberOfGroup.getRemarkname();
        if (!StringUtils.isEmpty(displayName)) {
            return CommonTools.emojiFormatter(displayName);
        }
        displayName = memberOfGroup.getDisplayname();
        if (!StringUtils.isEmpty(displayName)) {
            return CommonTools.emojiFormatter(displayName);
        }
        displayName = memberOfGroup.getNickname();
        if (!StringUtils.isEmpty(displayName)) {
            return CommonTools.emojiFormatter(displayName);
        }
        return userName;
    }


    public static String getSignatureNameOfGroup(Robot robot, String userName) {
        Contacts contacts = robot.getMemberMap().get(userName);
        if (contacts == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contacts.getSignature());
    }


    public static String getSignatureNameOfGroup(Contacts contacts) {
        if (contacts == null) {
            return null;
        }
        return CommonTools.emojiFormatter(contacts.getSignature());
    }

    /**
     * 根据用户名获取用户显示名称对应的拼音
     * 有备注显示备注，无备注显示昵称
     * 群则直接显示昵称
     *
     * @param userName 用户UserName
     * @return 备注
     */
    public static String getContactDisplayNameInitialByUserName(Robot robot, String userName) {
        Contacts contacts = robot.getMemberMap().get(userName);

        if (StringUtils.isNotEmpty(contacts.getRemarkpyinitial())) {
            return contacts.getRemarkpyinitial();
        }
        if (StringUtils.isNotEmpty(contacts.getPyinitial())) {
            return contacts.getPyinitial();
        } else {
            return "#";
        }
    }

    /**
     * 是否消息免打扰
     *
     * @param contacts 联系人
     * @return {@code false} 免打扰
     */
    public static boolean isMute(Contacts contacts) {
        if (isRoomContact(contacts.getUsername())) {
            return (contacts.getStatues() == null ||
                    contacts.getStatues().intValue() == WxConstant.ChatRoomMute.CHATROOM_NOTIFY_CLOSE.CODE);
        } else {
            return ((contacts.getContactflag().intValue() & WxConstant.ContactFlag.CONTACTFLAG_NOTIFYCLOSECONTACT.CODE) > 0);
        }


    }

    /**
     * 是否消息免打扰
     *
     * @param userName 联系人
     * @return {@code false} 免打扰
     */
    public static boolean isMute(Robot robot, String userName) {

        return isMute(robot.getMemberMap().get(userName));


    }

    /**
     * 是否为群
     *
     * @param userName 用户名
     * @return 是否为群
     */
    public static boolean isRoomContact(String userName) {
        return userName.startsWith("@@");
    }

    /**
     * 是否为群
     *
     * @param contacts 用户
     * @return 是否为群
     */
    public static boolean isRoomContact(Contacts contacts) {
        return isRoomContact(contacts.getUsername());
    }


}
