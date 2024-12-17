package com.t13max.wxbot.consts;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

public interface WxRespConstant {
    /**
     * 检测是否扫描登录接口 返回状态码
     *
     * @Author t13max
     * @Date 15:00 2024/12/16
     */
    @Getter
    enum CheckLoginResultCodeEnum {

        SUCCESS(200, "登录成功"),

        WAIT_CONFIRM(201, "请在手机上点击确认登录"),

        CANCEL(202, "用户取消"),

        WAIT_SCAN(400, "请使用微信扫一扫以登录,刷新二维码"),

        NONE(408, "请使用微信扫一扫以登录");
        private final Integer code;
        private final String msg;

        CheckLoginResultCodeEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static CheckLoginResultCodeEnum getByCode(Integer code) {
            for (CheckLoginResultCodeEnum value : CheckLoginResultCodeEnum.values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            throw new RuntimeException("未知类型：" + code);
        }

    }

    /**
     * 消息检测接口返回Code码
     */
    @Getter
    @Log4j2
    enum SyncCheckRetCodeEnum {

        SUCCESS(0, "成功"),
        LOGIN_OUT(1102, "退出"),
        LOGIN_OTHERWHERE(1101, "其它地方登陆"),
        UNKOWN(9999, "未知"),
        TICKET_ERROR(-14, "ticket错误"),
        PARAM_ERROR(1, "传入参数错误"),
        NOT_LOGIN_WARN(1100, "未登录提示"),
        LOGIN_ENV_ERROR(1203, "当前登录环境异常，为了安全起见请不要在web端进行登录"),
        TOO_OFEN(1205, "操作频繁");;


        private final Integer code;
        private final String type;

        SyncCheckRetCodeEnum(Integer code, String type) {
            this.code = code;
            this.type = type;
        }

        public static SyncCheckRetCodeEnum getByCode(Integer code) {
            for (SyncCheckRetCodeEnum value : SyncCheckRetCodeEnum.values()) {
                if (value.code.equals(code)) {
                    return value;
                }
            }
            log.error("未知类型：{}", code);
            return UNKOWN;
        }

    }

    /**
     *
     *
     * @Author t13max
     * @Date 15:01 2024/12/16
     */
    @Getter
    enum SyncCheckSelectorEnum {
        NORMAL("0", "正常"),
        NEW_MSG("2", "有新消息"),
        MOD_CONTACT("4", "删除新增好友"),
        UNKNOWN("3", "未知"),
        ADD_OR_DEL_CONTACT("6", "存在删除或者新增的好友信息"),
        ENTER_OR_LEAVE_CHAT("7", "进入或离开聊天界面"),
        DEFAULT("D", "");
        private String code;
        private String type;

        SyncCheckSelectorEnum(String code, String type) {
            this.code = code;
            this.type = type;
        }

        public static SyncCheckSelectorEnum getByCode(String code) {
            for (SyncCheckSelectorEnum value : SyncCheckSelectorEnum.values()) {
                if (value.getCode().equals(code)) {
                    return value;
                }
            }
            return SyncCheckSelectorEnum.DEFAULT;
        }

    }

    /**
     * 收到的消息类型
     *
     * @Author t13max
     * @Date 15:01 2024/12/16
     */
    @Getter
    enum WXReceiveMsgCodeEnum {
        MSG_TYPE_UNKNOWN(0, "未知消息类型"),
        MSG_TYPE_TEXT(1, "文本消息类型"),
        MSG_TYPE_IMAGE(3, "图片消息"),
        MSG_TYPE_VOICE(34, "语音消息"),
        MSG_TYPE_VIDEO(43, "小视频消息"),
        MSG_TYPE_MICROVIDEO(62, "短视频消息"),
        MSG_TYPE_EMOTICON(47, "表情消息"),
        MSG_TYPE_APP(49, "APP消息"),
        MSG_TYPE_LOCATION(48, "位置信息"),
        MSG_TYPE_STATUSNOTIFY(51, "系统通知"),
        MSG_TYPE_VERIFYMSG(37, "好友请求消息"),
        MSG_TYPE_SHARECARD(42, "名片分享消息"),
        MSG_TYPE_SYS(10000, "系统消息"),
        MSG_TYPE_RECALLED(10002, "撤回消息"),
        ;

        private final int code;
        private final String desc;

        WXReceiveMsgCodeEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public static WXReceiveMsgCodeEnum getByCode(int code) {
            for (WXReceiveMsgCodeEnum value : WXReceiveMsgCodeEnum.values()) {
                if (value.code == code) {
                    return value;
                }
            }
            return MSG_TYPE_UNKNOWN;
        }

    }

}
