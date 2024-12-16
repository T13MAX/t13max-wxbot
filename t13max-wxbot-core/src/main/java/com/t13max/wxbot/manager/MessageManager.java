package com.t13max.wxbot.manager;

import com.alibaba.fastjson.JSON;
import com.t13max.common.manager.ManagerBase;
import com.t13max.common.util.Log;
import com.t13max.wxbot.MessageHandler;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.WxReqParamsConstant;
import com.t13max.wxbot.consts.WxRespConstant;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.dto.request.WxSyncReq;
import com.t13max.wxbot.dto.response.SyncCheckResp;
import com.t13max.wxbot.dto.response.sync.AddMsgList;
import com.t13max.wxbot.dto.response.sync.RecommendInfo;
import com.t13max.wxbot.dto.response.sync.WebWxSyncResp;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.entity.Message;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.tools.ContactsTools;
import com.t13max.wxbot.tools.MessageTools;
import com.t13max.wxbot.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

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
        robot.setAlive(true);
        Runnable runnable = () -> {
            while (robot.isAlive()) {
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
                            continue;
                        }
                        case LOGIN_OUT:
                        case LOGIN_OTHERWHERE: {
                            Log.msg.warn(syncCheckRetCodeEnum.getType());
                            //重启客户端
                            //WeChatStater.restartApplication();
                            break;
                        }
                        case TICKET_ERROR:
                        case PARAM_ERROR:
                        case NOT_LOGIN_WARN:
                        case LOGIN_ENV_ERROR:
                        case TOO_OFEN: {
                            Log.msg.error(syncCheckRetCodeEnum.getType());
                            robot.setAlive(false);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.msg.error("消息同步错误：{}", e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                }

            }
        };
        ExecutorServiceUtil.getReceivingExecutorService().execute(runnable);
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
        WxRespConstant.WXReceiveMsgCodeEnum msgType = WxRespConstant.WXReceiveMsgCodeEnum.getByCode(msg.getMsgType());
        //=============地图消息，特殊处理=============
        if (msgType == WxRespConstant.WXReceiveMsgCodeEnum.MSGTYPE_TEXT && !StringUtils.isEmpty(msg.getUrl())) {
            //地图消息 地图消息的发送
            msg.setMsgType(WxRespConstant.WXReceiveMsgCodeEnum.MSGTYPE_MAP.getCode());
            msgType = WxRespConstant.WXReceiveMsgCodeEnum.MSGTYPE_MAP;
        }
        //=============群消息处理=============
        groupMsgFormat(robot, msg);

        msg.setType(msgType);

        //=============如果是当前房间 发送已读通知==============
        /*if (msg.getFromUserName().equals(ChatPanelContainer.getCurrRoomId())) {
            ExecutorServiceUtil.getGlobalExecutorService().execute(() -> MessageTools.sendStatusNotify(msg.getFromUserName()));
        }*/

        //下载资源后缀
        String ext = null;
        //下载资源文件名
        String fileName = msg.getMsgId();
        //存储的消息
        Message message = new Message();

        switch (msgType) {
            case MSGTYPE_MAP: {
                Map<String, Object> map = XmlStreamUtil.toMap(msg.getOriContent());
                String thumbUrl = WxURLEnum.BASE_URL.getUrl() + msg.getContent();
                String url = msg.getUrl();
                String title = map.get("msg.location.attr.poiname").toString();
                String subTitle = map.get("msg.location.attr.label").toString();

                msg.setPlainText("[地图]" + title);
                ext = ".gif";
                //downloadFile(msg, fileName, ext);
                //message = newMsgToDBMessage(msg);
                message.setMsgType(WxRespConstant.WXReceiveMsgCodeEnum.MSGTYPE_APP.getCode());
                message.setAppMsgType(WxRespConstant.WXReceiveMsgCodeOfAppEnum.PICTURE.getType());
                message.setThumbUrl(msg.getFilePath());
                message.setUrl(url);
                message.setTitle(title);

            }
            break;
            case MSGTYPE_TEXT:
                //消息格式化
                CommonTools.emojiFormatter(msg);
                textMsgFormat(robot, msg);
                //文本消息
                msg.setPlainText(msg.getContent());
                break;
            case MSGTYPE_IMAGE:
                msg.setPlainText("[图片]");
                ext = ".gif";
                //存储消息
                //downloadThumImg(msg, fileName, ext);
                //downloadFile(msg, fileName, ext);
                //message = newMsgToDBMessage(msg);
                break;
            case MSGTYPE_VOICE:
                ext = ".mp3";
                msg.setPlainText("[语音]");
                break;
            case MSGTYPE_VIDEO:
            case MSGTYPE_MICROVIDEO:
                ext = ".mp4";
                msg.setPlainText("[视频]");

                break;
            case MSGTYPE_EMOTICON:
                msg.setPlainText("[表情]");
                ext = ".gif";
                break;
            case MSGTYPE_APP: {
                Map<String, Object> map = new HashMap<>();
                try {
                    map = XmlStreamUtil.toMap(msg.getContent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msg.setContentMap(map);
                Object desc = map.get("msg.appmsg.des");
                Object url = map.get("msg.appmsg.url");
                Object title = map.get("msg.appmsg.title");
                Object thumbUrl = map.get("msg.appmsg.thumburl");
                Object sourceIconUrl = map.get("msg.appmsg.weappinfo.weappiconurl");
                Object sourceName = map.get("msg.appmsg.sourcedisplayname");
                Object height = map.get("msg.appmsg.appattach.cdnthumbheight");
                Object width = map.get("msg.appmsg.appattach.cdnthumbwidth");

                switch (WxRespConstant.WXReceiveMsgCodeOfAppEnum.getByCode(msg.getAppMsgType())) {
                    case LINK:
                        msg.setPlainText("[链接]" + title);
                        break;
                    case FILE:
                        if (title != null) {
                            fileName = title.toString();
                        }
                        int i = msg.getFileName().lastIndexOf(".");
                        if (i != -1) {
                            ext = msg.getFileName().substring(i);
                        }
                        msg.setPlainText("[文件]" + title);
                        break;
                    default:
                    case PROGRAM:
                        msg.setPlainText("[小程序]" + title);
                        break;
                    case MUSIC:
                        msg.setPlainText("[音乐]" + title);
                    case PICTURE:
                        sourceName = map.get("msg.appinfo.appname");
                        ext = ".gif";
                        msg.setPlainText("[小程序]图片");
                        break;
                    case TRANSFER:
                        msg.setPlainText(desc == null ? "[微信转账]" : desc.toString());
                        msg.setAppMsgType(WxRespConstant.WXReceiveMsgCodeOfAppEnum.LINK.getType());
                        break;
                }
                message.setTitle(title == null ? null : title.toString());
                message.setDesc(desc == null ? null : desc.toString());
                message.setImgWidth(width == null ? null : Integer.parseInt(width.toString()));
                message.setImgHeight(height == null ? null : Integer.parseInt(height.toString()));
                message.setThumbUrl(thumbUrl == null ? null : thumbUrl.toString());
                message.setUrl(url == null ? null : url.toString());
                message.setSourceIconUrl(sourceIconUrl == null ? null : sourceIconUrl.toString());
                message.setSourceName(sourceName == null ? null : sourceName.toString());
            }
            break;
            case MSGTYPE_VOIPMSG:
                break;
            case MSGTYPE_VOIPNOTIFY:
                break;
            case MSGTYPE_VOIPINVITE:
                break;
            case MSGTYPE_LOCATION:
                msg.setPlainText("[位置，请在手机上查看]");
                break;
            case MSGTYPE_SYS: {
                msg.setPlainText(msg.getContent());
                break;
            }
            case MSGTYPE_STATUSNOTIFY: {
                break;
            }
            case MSGTYPE_SYSNOTICE:
                break;
            case MSGTYPE_POSSIBLEFRIEND_MSG:
                break;
            case MSGTYPE_VERIFYMSG: {
                msg.setPlainText("[好友申请]");
                Map<String, Object> map = new HashMap<>();
                try {
                    map = XmlStreamUtil.toMap(msg.getContent());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Object thumbUrl = map.get("msg.attr.smallheadimgurl");
                Object headImgUrl = map.get("msg.attr.bigheadimgurl");
                Object id = map.get("msg.attr.alias");
                RecommendInfo recommendInfo = msg.getRecommendInfo();
                message.setContactsNickName(recommendInfo.getNickName());
                message.setContactsId(id == null ? null : id.toString());
                message.setContactsProvince(recommendInfo.getProvince());
                message.setContactsCity(recommendInfo.getCity());
                message.setContactsSex(recommendInfo.getSex());
                message.setThumbUrl(thumbUrl == null ? null : thumbUrl.toString());
                message.setContactsUserName(recommendInfo.getUserName());
                message.setContactsTicket(recommendInfo.getTicket());
                message.setContactsHeadImgUrl(headImgUrl == null ? null : headImgUrl.toString());
                break;
            }
            case MSGTYPE_SHARECARD: {

                msg.setPlainText("[名片消息]");
                MessageTools.setMessageCardField(msg.getContent(), message);

            }
            break;
            case MSGTYPE_RECALLED: {
                Map<String, Object> map = XmlStreamUtil.toMap(msg.getContent());
                msg.setContentMap(map);
                msg.setPlainText(map.get("sysmsg.revokemsg.replacemsg").toString());
            }
            break;
            case UNKNOWN:
            default:
                //log.warn(LogUtil.printFromMeg(msg, msgType.getCode()));
                break;
        }
        if (message.getFromUsername().startsWith("@@") || message.getToUsername().startsWith("@@")) {
            message.setGroup(true);
        } else {
            message.setGroup(false);
        }
        //显示的名称
        if (message.isGroup()) {
            //如果是群则显示群成员名称
            message.setPlainName(message.getFromMemberOfGroupDisplayname());
        } else {
            message.setPlainName(ContactsTools.getContactDisplayNameByUserName(robot, message.getFromUsername()));
        }
        List<MessageHandler> messageHandlers = this.messageHandlerList.get(robot);
        if (messageHandlers != null) {
            for (MessageHandler messageHandler : messageHandlers) {
                messageHandler.handle(message);
            }
        }
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
