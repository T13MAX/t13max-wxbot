package com.t13max.wxbot.manager;

import com.alibaba.fastjson.JSON;
import com.t13max.common.manager.ManagerBase;
import com.t13max.common.util.Log;
import com.t13max.util.TimeUtil;
import com.t13max.wxbot.MessageHandler;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.RobotStatusEnum;
import com.t13max.wxbot.consts.WxReqParamsConstant;
import com.t13max.wxbot.consts.WxRespConstant;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.dto.request.WxSyncReq;
import com.t13max.wxbot.dto.response.SyncCheckResp;
import com.t13max.wxbot.dto.response.msg.send.WebWXSendMsgResponse;
import com.t13max.wxbot.dto.response.sync.AddMsgList;
import com.t13max.wxbot.dto.response.sync.WebWxSyncResp;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.entity.Message;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.tools.ContactsTools;
import com.t13max.wxbot.utils.*;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 消息管理器
 *
 * @author t13max
 * @since 14:47 2024/12/16
 */
public class MessageManager extends ManagerBase {

    //消息回调处理器
    private final Map<Robot, List<MessageHandler>> messageHandlerList = new ConcurrentHashMap<>();

    private final Set<String> msgIds = new HashSet<>();

    public static MessageManager inst() {
        return inst(MessageManager.class);
    }

    /**
     * 注册一个消息处理器
     *
     * @Author t13max
     * @Date 15:19 2024/12/16
     */
    public void register(Robot robot, MessageHandler messageHandler) {
        List<MessageHandler> messageHandlers = messageHandlerList.computeIfAbsent(robot, k -> new CopyOnWriteArrayList<>());
        messageHandlers.add(messageHandler);
    }

    /**
     * 处理成功消息
     *
     * @Author t13max
     * @Date 15:12 2024/12/16
     */
    private void processSuccessMsg(Robot robot, String selector) throws Exception {
        // 最后收到正常报文时间
        robot.setLastNormalRetCodeTime(System.currentTimeMillis());
        //消息同步
        //JSONObject msgObj = webWxSync();
        WebWxSyncResp webWxSyncMsg = webWxSync(robot);

        switch (WxRespConstant.SyncCheckSelectorEnum.getByCode(selector)) {
            case NORMAL:
                break;
            case MOD_CONTACT:
            case ADD_OR_DEL_CONTACT:
            case NEW_MSG:

                //新消息
                for (AddMsgList msg : webWxSyncMsg.getAddMsgList()) {
                    if (msgIds.contains(msg.getMsgId())) {
                        Log.msg.warn("消息重复：{}", msg);
                        continue;
                    }
                    msgIds.add(msg.getMsgId());
                    ExecutorServiceUtil.getGlobalExecutorService().execute(() -> {
                        handleNewMsg(robot, msg);
                    });
                }
                //联系人修改
                ContactsManager.inst().handleModContact(robot, webWxSyncMsg.getModContactList());
                for (Contacts contacts : webWxSyncMsg.getDelContactList()) {
                    Log.msg.info("联系人删除：{}", contacts);
                }

                break;

            case ENTER_OR_LEAVE_CHAT:
                webWxSync(robot);
                break;

            case UNKNOWN:
                Log.msg.info("未知消息：{}", webWxSyncMsg);
                break;
            default:
                break;

        }
    }

    public void startReceiving(Robot robot) {
        try {
            //检测是否有新消息
            SyncCheckResp syncCheckResp = syncCheck(robot);
            WxRespConstant.SyncCheckRetCodeEnum syncCheckRetCodeEnum = WxRespConstant.SyncCheckRetCodeEnum.getByCode(syncCheckResp.getRetCode());
            switch (syncCheckRetCodeEnum) {
                case SUCCESS: {
                    processSuccessMsg(robot, syncCheckResp.getSelector());
                    break;
                }
                case UNKOWN: {
                    Log.msg.info(syncCheckRetCodeEnum.getType());
                    return;
                }
                case TICKET_ERROR:
                case PARAM_ERROR:
                case NOT_LOGIN_WARN:
                case LOGIN_ENV_ERROR:
                case TOO_OFEN, LOGIN_OTHERWHERE, LOGIN_OUT: {
                    Log.msg.error(syncCheckRetCodeEnum.getType());
                    robot.changeStatus(RobotStatusEnum.IDLE);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.msg.error("消息同步错误：{}", e.getMessage());
        }
    }

    /**
     * 同步消息
     *
     * @Author t13max
     * @Date 15:16 2024/12/16
     */
    private WebWxSyncResp webWxSync(Robot robot) throws Exception {
        String url = String.format(WxURLEnum.WEB_WX_SYNC_URL.getUrl(),
                robot.getLoginResultData().getUrl(),
                robot.getLoginResultData().getBaseRequest().getWxSid(),
                robot.getLoginResultData().getBaseRequest().getSKey(),
                robot.getLoginResultData().getPassTicket());

        WxSyncReq wxSyncReq = WxSyncReq.builder().SyncKey(robot.getLoginResultData().getSyncKeyObject())
                .rr(-System.currentTimeMillis() / 1000)
                .BaseRequest(robot.getLoginResultData().getBaseRequest()).build();
        String paramStr = JSON.toJSONString(wxSyncReq);


        HttpEntity entity = HttpUtil.doPost(url, paramStr);
        String text = EntityUtils.toString(entity, Consts.UTF_8);
        WebWxSyncResp webWxSyncMsg = JSON.parseObject(text, WebWxSyncResp.class);
        if (webWxSyncMsg.getBaseResponse().getRet() != 0) {
            throw new RobotException("消息同步失败！");
        } else {
            robot.getLoginResultData().setSyncCheckKey(webWxSyncMsg.getSyncCheckKey());
            robot.getLoginResultData().setSyncKey(
                    webWxSyncMsg.getSyncKey()
                            .getList()
                            .stream()
                            .map(e -> e.getKey() + "_" + e.getVal())
                            .collect(Collectors.joining("|"))
            );
            robot.getLoginResultData().setSyncKeyObject(webWxSyncMsg.getSyncKey());
        }
        return webWxSyncMsg;
    }

    /**
     * 检查是否有新消息
     *
     * @Author t13max
     * @Date 15:17 2024/12/16
     */
    private SyncCheckResp syncCheck(Robot robot) throws Exception {
        // 组装请求URL和参数
        String url = String.format(WxURLEnum.SYNC_CHECK_URL.getUrl(), robot.getLoginResultData().getSyncUrl());
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.R.para(), String.valueOf(System.currentTimeMillis())));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.S_KEY.para(), robot.getLoginResultData().getBaseRequest().getSKey()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.SID.para(), robot.getLoginResultData().getBaseRequest().getWxSid()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.UIN.para(), robot.getLoginResultData().getBaseRequest().getWxUin()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.DEVICE_ID.para(), robot.getLoginResultData().getBaseRequest().getDeviceId()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.SYNC_KEY.para(), robot.getLoginResultData().getSyncKey()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.SyncCheckParaEnum.UNDER_LINE.para(), String.valueOf(System.currentTimeMillis())));
        SleepUtils.sleep(7);
        HttpEntity entity = HttpUtil.doGetOfReceive(url, params, true, null);
        if (entity == null) {
            throw new Exception("Entity is null!");
        }
        String result = EntityUtils.toString(entity);
        String regEx = "window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}";
        Matcher matcher = CommonTools.getMatcher(regEx, result);
        if (!matcher.find()) {
            throw new Exception("Unexpected sync check result: " + result);
        } else {
            return SyncCheckResp.builder().retCode(Integer.parseInt(matcher.group(1)))
                    .selector(matcher.group(2)).build();
        }
    }

    /**
     * 处理新消息
     *
     * @Author t13max
     * @Date 15:36 2024/12/16
     */
    public void handleNewMsg(Robot robot, AddMsgList msg) {
        //消息类型封装
        WxRespConstant.WXReceiveMsgCodeEnum MSG_TYPE = WxRespConstant.WXReceiveMsgCodeEnum.getByCode(msg.getMsgType());

        //=============群消息处理=============
        groupMsgFormat(robot, msg);

        msg.setType(MSG_TYPE);

        //存储的消息
        Message message = null;

        switch (MSG_TYPE) {
            case MSG_TYPE_TEXT:
                //消息格式化
                CommonTools.emojiFormatter(msg);
                textMsgFormat(robot, msg);
                //文本消息
                msg.setPlainText(msg.getContent());
                message = newMsgToDBMessage(robot, msg);
                break;
            case MSG_TYPE_IMAGE:
                msg.setPlainText("[图片]");
                message = newMsgToDBMessage(robot, msg);
                break;
            case MSG_TYPE_VOICE:
                msg.setPlainText("[语音]");
                message = newMsgToDBMessage(robot, msg);
                break;
            case MSG_TYPE_VIDEO:
            case MSG_TYPE_MICROVIDEO:
                msg.setPlainText("[视频]");
                message = newMsgToDBMessage(robot, msg);
                break;
            case MSG_TYPE_EMOTICON:
                msg.setPlainText("[表情]");
                message = newMsgToDBMessage(robot, msg);
                break;
            case MSG_TYPE_APP: {
                msg.setPlainText("[APP]");
                message = newMsgToDBMessage(robot, msg);
            }
            case MSG_TYPE_LOCATION:
                msg.setPlainText("[位置，请在手机上查看]");
                break;
            case MSG_TYPE_SYS: {
                msg.setPlainText(msg.getContent());
                break;
            }
            case MSG_TYPE_VERIFYMSG: {
                msg.setPlainText("[好友申请]");
                message = newMsgToDBMessage(robot, msg);
                break;
            }
            case MSG_TYPE_SHARECARD: {
                msg.setPlainText("[名片消息]");
                message = newMsgToDBMessage(robot, msg);
            }
            case MSG_TYPE_RECALLED: {
                Map<String, Object> map = XmlStreamUtil.toMap(msg.getContent());
                msg.setContentMap(map);
                msg.setPlainText(map.get("sysmsg.revokemsg.replacemsg").toString());
                message = newMsgToDBMessage(robot, msg);
            }
            default:

        }
        if (message != null) {
            String fromUsername = message.getFromUsername();
            message.setGroup(fromUsername.startsWith("@@") || message.getToUsername().startsWith("@@"));
            //显示的名称
            if (message.isGroup()) {
                //如果是群则显示群成员名称
                message.setPlainName(message.getFromMemberOfGroupDisplayname());
            } else {
                message.setPlainName(ContactsTools.getContactDisplayNameByUserName(robot, fromUsername));
            }
            Set<String> recentContacts = robot.getRecentContacts();
            recentContacts.add(fromUsername);
            List<MessageHandler> messageHandlers = this.messageHandlerList.get(robot);
            if (messageHandlers != null) {
                for (MessageHandler messageHandler : messageHandlers) {
                    messageHandler.handle(message);
                }
            }
        }
    }

    private Message newMsgToDBMessage(Robot robot, AddMsgList msg) {
        boolean isFromSelf = msg.getFromUserName().endsWith(robot.getUserName());
        boolean isToSelf = msg.getToUserName().endsWith(robot.getUserName());
        return Message
                .builder()
                .plaintext(msg.getPlainText() == null ? msg.getContent() : msg.getPlainText())
                .content(msg.getContent())
                .filePath(msg.getFilePath())
                .createTime(TimeUtil.formatTimestamp(TimeUtil.nowMills()))
                .fromNickname(isFromSelf ? robot.getNickName() : ContactsTools.getContactNickNameByUserName(robot, msg.getFromUserName()))
                .fromRemarkname(isFromSelf ? robot.getNickName() : ContactsTools.getContactRemarkNameByUserName(robot, msg.getFromUserName()))
                .fromUsername(msg.getFromUserName())
                .id(UUID.randomUUID().toString().replace("-", ""))
                .toNickname(isToSelf ? robot.getNickName() : ContactsTools.getContactNickNameByUserName(robot, msg.getToUserName()))
                .toRemarkname(isToSelf ? robot.getNickName() : ContactsTools.getContactRemarkNameByUserName(robot, msg.getToUserName()))
                .toUsername(msg.getToUserName())
                .msgId(msg.getMsgId())
                .msgType(msg.getMsgType())
                .isSend(isFromSelf)
                .appMsgType(msg.getAppMsgType())
                .msgJson(JSON.toJSONString(msg))
                .msgDesc(WxRespConstant.WXReceiveMsgCodeEnum.getByCode(msg.getMsgType()).getDesc())
                .fromMemberOfGroupDisplayname(msg.isGroupMsg() && !msg.getFromUserName().equals(robot.getUserName())
                        ? ContactsTools.getMemberDisplayNameOfGroup(robot, msg.getFromUserName(), msg.getMemberName()) : null)
                .fromMemberOfGroupNickname(msg.isGroupMsg() && !msg.getFromUserName().equals(robot.getUserName())
                        ? ContactsTools.getMemberNickNameOfGroup(robot, msg.getFromUserName(), msg.getMemberName()) : null)
                .fromMemberOfGroupUsername(msg.isGroupMsg() && !msg.getFromUserName().equals(robot.getUserName())
                        ? msg.getMemberName() : null)
                .slavePath(msg.getSlavePath())
                .response(JSON.toJSONString(WebWXSendMsgResponse.builder()
                        .BaseResponse(WebWXSendMsgResponse.BaseResponse.builder().Ret(0).build())
                        .LocalID(msg.getMsgId())
                        .MsgID(msg.getNewMsgId() + "").build()))
                .playLength(msg.getPlayLength())
                .imgHeight(msg.getImgHeight())
                .imgWidth(msg.getImgWidth())
                .voiceLength(msg.getVoiceLength())
                .fileName(msg.getFileName())
                .fileSize(msg.getFileSize())
                .contentMap(msg.getContentMap())
                .messageTime(LocalDateTime.now())
                .oriContent(msg.getOriContent())
                .build();
    }

    private void groupMsgFormat(Robot robot, AddMsgList msg) {
        // 群消息与普通消息不同的是在其消息体（Content）中会包含发送者id及":<br/>"消息，
        // 这里需要处理一下，去掉多余信息，只保留消息内容
        //"群成员UserName:<br/>消息内容"
        if (!msg.getFromUserName().startsWith("@@") && !msg.getToUserName().startsWith("@@")) {
            return;
        }
        msg.setGroupMsg(Boolean.TRUE);
        if (msg.getFromUserName().equals(robot.getUserName())) {
            msg.setMemberName(robot.getUserName());
        } else {
            String content = msg.getContent();
            int index = content.indexOf(":<br/>");
            if (index != -1) {
                msg.setContent(content.substring(index + ":<br/>".length()));
                //发送消息的人
                msg.setMemberName(content.substring(0, index));
            }
        }
    }

    private void textMsgFormat(Robot robot, AddMsgList msg) {
        String content = msg.getContent();
        //获取自己在群里的备注
        String groupMyUserNickNameOfGroup = ContactsTools.getMemberDisplayNameOfGroup(robot, msg.getFromUserName(), robot.getUserName());
        //判断是否@自己
        if (groupMyUserNickNameOfGroup != null
                && content.contains("@" + groupMyUserNickNameOfGroup + " ")) {
            msg.setMentionMe(true);
            //消息发送成员昵称
            String groupOtherUserNickNameOfGroup =
                    ContactsTools.getMemberDisplayNameOfGroup(robot, msg.getFromUserName(), msg.getMemberName());
            msg.setMentionMeUserNickName(groupOtherUserNickNameOfGroup);
        }
        //@用户后面的空白符 JLabel不支持显示  需要替换
        msg.setContent(content.replace(" ", " "));
    }
}
