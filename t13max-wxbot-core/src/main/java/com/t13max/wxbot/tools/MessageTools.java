package com.t13max.wxbot.tools;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.t13max.common.util.Log;
import com.t13max.wxbot.consts.WxReqParamsConstant;
import com.t13max.wxbot.consts.WxRespConstant;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.dto.request.msg.send.*;
import com.t13max.wxbot.dto.response.msg.send.WebWXSendMsgResponse;
import com.t13max.wxbot.entity.Message;
import com.t13max.wxbot.utils.HttpUtil;
import com.t13max.wxbot.utils.XmlStreamUtil;
import com.t13max.wxbot.Robot;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * 消息处理类
 *
 * @author SXS
 * @version 1.1
 * @date 创建时间：2017年4月23日 下午2:30:37
 */
@Log4j2
public class MessageTools {

    /**
     * 根据指定类型发送消息
     *
     * @Author t13max
     * @Date 16:13 2024/12/16
     */
    public static WebWXSendMsgResponse sendMsgByUserId(Robot robot, List<Message> messages) {
        if (messages == null) {
            return WebWXSendMsgResponse.error("messages is null");
        }
        WebWXSendMsgResponse sendMsgResponse = null;

        try {
            for (Message message : messages) {
                String toUserName = message.getToUsername();
                if (StringUtils.isEmpty(toUserName)) {
                    log.error("消息接收者为空：{}", message);
                    return WebWXSendMsgResponse.error("toUserName is null");

                }

                String content = XmlStreamUtil.formatXml(message.getContent());
                WxRespConstant.WXReceiveMsgCodeEnum byCode = WxRespConstant.WXReceiveMsgCodeEnum.getByCode(message.getMsgType());
                switch (byCode) {
                    case MSGTYPE_TEXT:
                        sendMsgResponse = sendTextMsgByUserId(robot, content, toUserName);
                        break;
                    case MSGTYPE_MAP:
                        sendMsgResponse = sendMapMsgByUserId(robot, toUserName, content);
                        break;
                    case MSGTYPE_SHARECARD:
                        sendMsgResponse = sendCardMsgByUserId(robot, toUserName, content);
                        break;
                    case MSGTYPE_IMAGE, MSGTYPE_VIDEO, MSGTYPE_EMOTICON:
                    default:
                        Log.msg.error("不支持的类型");
                }
                if (sendMsgResponse == null) {
                    log.error("发送消息失败：{}", message);
                    return WebWXSendMsgResponse.error("null");
                } else if (sendMsgResponse.getBaseResponse().getRet() != 0) {
                    log.error("发送消息失败：{},{}", sendMsgResponse.getBaseResponse().getErrMsg(), message);
                    return sendMsgResponse;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("发送消息失败：{}", e.getMessage());
            return WebWXSendMsgResponse.error(e.getMessage());

        }
        return sendMsgResponse;
    }

    /**
     * 根据指定类型发送消息
     *
     * @param message 消息列表
     */
    public static WebWXSendMsgResponse sendMsgByUserId(Robot robot, Message message) {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        return sendMsgByUserId(robot, messages);
    }

    /**
     * 根据UserName发送文本消息
     *
     * @param toUserName 消息接收者UserName
     * @param content    消息内容，content可能包含资源文件的id等信息，可直接使用
     * @return {@link WebWXSendMsgResponse}
     * @author SXS
     * @date 2017年5月4日 下午11:17:38
     */
    private static WebWXSendMsgResponse sendTextMsgByUserId(Robot robot, String content, String toUserName) throws IOException {
        String url = String.format(WxURLEnum.WEB_WX_SEND_MSG.getUrl(), robot.getLoginResultData().getUrl());
        WebWXSendMsgRequest msgRequest = new WebWXSendMsgRequest();
        WebWXSendingMsg textMsg = new WebWXSendingTextMsg();
        textMsg.FromUserName = robot.getUserName();
        textMsg.Content = content;
        textMsg.ToUserName = toUserName;
        msgRequest.Msg = textMsg;
        return sendMsg(robot, msgRequest, url);
    }

    /**
     * 根据UserName发送地图消息
     *
     * @param toUserName 消息接收者UserName
     * @param content    消息内容，
     * @return {@link WebWXSendMsgResponse}
     * @author SXS
     * @date 2021年9月26日 下午14:18:38
     */
    private static WebWXSendMsgResponse sendMapMsgByUserId(Robot robot, String toUserName, String content) throws IOException {
        String url = String.format(WxURLEnum.WEB_WX_SEND_MSG.getUrl(), robot.getLoginResultData().getUrl());
        WebWXSendMsgRequest msgRequest = new WebWXSendMsgRequest();
        WebWXSendingMsg textMsg = new WebWXSendingMapMsg();
        textMsg.Content = content;
        textMsg.ToUserName = toUserName;
        msgRequest.Msg = textMsg;
        return sendMsg(robot, msgRequest, url);
    }

    /**
     * 根据UserName发送名片消息
     *
     * @param toUserName 消息接收者UserName
     * @param content    消息内容，content可能包含资源文件的id等信息，可直接使用
     * @return {@link WebWXSendMsgResponse}
     * @author SXS
     * @date 2017年5月4日 下午11:17:38
     */
    private static WebWXSendMsgResponse sendCardMsgByUserId(Robot robot, String toUserName, String content) throws IOException {
        String url = String.format(WxURLEnum.WEB_WX_SEND_MSG.getUrl(), robot.getLoginResultData().getUrl());
        WebWXSendMsgRequest msgRequest = new WebWXSendMsgRequest();
        WebWXSendingCardMsg textMsg = new WebWXSendingCardMsg();
        textMsg.Content = content;
        textMsg.ToUserName = toUserName;
        msgRequest.Msg = textMsg;
        return sendMsg(robot, msgRequest, url);
    }


    /**
     * 根据用户id发送撤回消息
     *
     * @param userId      消息接收者
     * @param clientMsgId 发送消息返回的 LocalID {@link WebWXSendMsgResponse}
     * @param svrMsgId    发送消息返回的 MsgId {@link WebWXSendMsgResponse}
     * @return {@code true} 发送成功 {@code false} 发送失败
     * @author SXS
     * @date 201714年5月7日 下午10:34:24
     */
    public static boolean sendRevokeMsgByUserId(Robot robot, String userId, String clientMsgId, String svrMsgId) {

        String url = String.format(WxURLEnum.WEB_WX_REVOKE_MSG.getUrl()
                , robot.getLoginResultData().getUrl()
                , robot.getLoginResultData().getPassTicket());

        WebWXSendingRevokeMsg webWXSendingRevokeMsg = new WebWXSendingRevokeMsg();
        webWXSendingRevokeMsg.ClientMsgId = clientMsgId;
        webWXSendingRevokeMsg.SvrMsgId = svrMsgId;
        webWXSendingRevokeMsg.ToUserName = userId;
        webWXSendingRevokeMsg.BaseRequest = robot.getLoginResultData().getBaseRequest();
        String paramStr = JSON.toJSONString(webWXSendingRevokeMsg);
        HttpEntity entity = HttpUtil.doPost(url, paramStr);
        if (entity != null) {
            try {
                String result = EntityUtils.toString(entity, Consts.UTF_8);
                return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
            } catch (Exception e) {
                log.error("webWxSendMsgImg 错误： {}", e.getMessage());
            }
        }
        return false;
    }

    /**
     * 发送状态通知
     *
     * @Author t13max
     * @Date 15:38 2024/12/16
     */
    public static WebWXSendMsgResponse sendStatusNotify(Robot robot, String toUserName) {
        String url = String.format(WxURLEnum.WEB_WX_SEND_NOTIFY_MSG.getUrl(), robot.getLoginResultData().getUrl());
        WebWXSendingNotifyMsg webWXSendingNotifyMsg = new WebWXSendingNotifyMsg();
        webWXSendingNotifyMsg.Code = 1;
        webWXSendingNotifyMsg.FromUserName = robot.getUserName();
        webWXSendingNotifyMsg.ToUserName = toUserName;
        String paramStr = JSON.toJSONString(webWXSendingNotifyMsg);
        HttpEntity entity = HttpUtil.doPost(url, paramStr);
        if (entity != null) {
            try {
                String result = EntityUtils.toString(entity, Consts.UTF_8);
                return JSON.parseObject(result, WebWXSendMsgResponse.class);
            } catch (Exception e) {
                log.error("webWxSendMsgImg 错误： {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 被动添加好友
     *
     * @param userName 用户名
     * @param ticket   ticket
     * @date 2017年6月29日 下午10:08:43
     */
    public static WebWXSendMsgResponse addFriend(Robot robot, String userName, String ticket) {

        // 接受好友请求
        int status = WxReqParamsConstant.VerifyFriendEnum.ACCEPT.getCode();


        String url = String.format(WxURLEnum.WEB_WX_VERIFYUSER.getUrl(), robot.getLoginResultData().getUrl(),
                System.currentTimeMillis() / 3158L, robot.getLoginResultData().getPassTicket());

        List<Map<String, Object>> verifyUserList = new ArrayList<Map<String, Object>>();
        Map<String, Object> verifyUser = new HashMap<String, Object>();
        verifyUser.put("Value", userName);
        verifyUser.put("VerifyUserTicket", ticket);
        verifyUserList.add(verifyUser);

        List<Integer> sceneList = new ArrayList<Integer>();
        sceneList.add(33);

        JSONObject body = new JSONObject();
        body.put("BaseRequest", robot.getLoginResultData().getBaseRequest());
        body.put("Opcode", status);
        body.put("VerifyUserListSize", 1);
        body.put("VerifyUserList", verifyUserList);
        body.put("VerifyContent", "");
        body.put("SceneListCount", 1);
        body.put("SceneList", sceneList);
        body.put("skey", robot.getLoginResultData().getBaseRequest().getSKey());
        String result = null;
        try {
            String paramStr = JSON.toJSONString(body);
            HttpEntity entity = HttpUtil.doPost(url, paramStr);
            result = EntityUtils.toString(entity, Consts.UTF_8);
            log.info("自动添加好友：" + result);
            return JSON.parseObject(result, WebWXSendMsgResponse.class);

        } catch (Exception e) {
            log.error("webWxSendMsg", e);
            return WebWXSendMsgResponse.error(e.getMessage());
        }


    }

    /**
     * 修改联系人备注
     *
     * @param userName   用户id
     * @param remarkName 备注名称
     * @return 参数
     */
    public static WebWXSendMsgResponse modifyRemarkName(Robot robot, String userName, String remarkName) throws IOException {
        String url = String.format(WxURLEnum.WEB_WX_REMARKNAME.getUrl(), robot.getLoginResultData().getUrl());
        WebWXSendMsgRequest msgRequest = new WebWXSendMsgRequest();
        WebWXModifyRemarkNameMsg msg = new WebWXModifyRemarkNameMsg();
        msg.CmdId = 2;
        msg.RemarkName = remarkName;
        msg.UserName = userName;
        msgRequest.Msg = msg;
        return sendMsg(robot, msgRequest, url);
    }

    /**
     * 发送消息
     *
     * @param webWXSendMsgRequest 请求体
     * @param url                 请求地址
     * @return {@link WebWXSendMsgResponse}
     * @throws IOException IOException
     */
    private static WebWXSendMsgResponse sendMsg(Robot robot, WebWXSendMsgRequest webWXSendMsgRequest, String url) throws IOException {
        webWXSendMsgRequest.BaseRequest = robot.getLoginResultData().getBaseRequest();
        String paramStr = JSON.toJSONString(webWXSendMsgRequest);

        HttpEntity entity = HttpUtil.doPost(url, paramStr);
        if (entity == null) {
            return WebWXSendMsgResponse.error("response is null.");
        }
        String s = EntityUtils.toString(entity, Consts.UTF_8);
        return JSON.parseObject(s, WebWXSendMsgResponse.class);
    }

    /**
     * 随机生成MessageId
     *
     * @return
     */
    public static String randomMessageId() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return raw;
    }

    /**
     * 将 卡片消息的xml提取到各个字段
     *
     * @param content xml
     * @param message 消息
     */
    public static Map<String, Object> setMessageCardField(String content, Message message) {
        Map<String, Object> map = new HashMap<>();
        try {
            map = XmlStreamUtil.toMap(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object title = map.get("msg.attr.nickname");
        Object thumbUrl = map.get("msg.attr.smallheadimgurl");
        Object headImgUrl = map.get("msg.attr.bigheadimgurl");
        Object id = map.get("msg.attr.alias");
        Object province = map.get("msg.attr.province");
        Object city = map.get("msg.attr.city");
        Object sex = map.get("msg.attr.sex");
        Object userName = map.get("msg.attr.username");

        message.setContactsNickName(title == null ? null : title.toString());
        message.setContactsId(id == null ? null : id.toString());
        message.setContactsProvince(province == null ? null : province.toString());
        message.setContactsCity(city == null ? null : city.toString());
        message.setContactsSex(sex == null ? null : Byte.valueOf(sex.toString()));
        message.setThumbUrl(thumbUrl == null ? null : thumbUrl.toString());
        message.setContactsUserName(userName == null ? null : userName.toString());
        message.setContactsHeadImgUrl(headImgUrl == null ? null : headImgUrl.toString());
        return map;
    }
}
