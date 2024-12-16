/**
 * Copyright 2021 bejson.com
 */
package com.t13max.wxbot.dto.response.sync;


import com.t13max.wxbot.consts.WxRespConstant;
import lombok.Data;

import java.util.Map;

/**
 * Auto-generated: 2021-02-22 13:35:59
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class AddMsgList {

    private String MsgId;
    private String FromUserName;
    private String ToUserName;
    private int MsgType;
    private String Content;
    private int Status;
    private int ImgStatus;
    private long CreateTime;
    private long VoiceLength;
    private long PlayLength;
    private String FileName;
    private Long FileSize;
    private String MediaId;
    private String Url;
    private int AppMsgType;
    private int StatusNotifyCode;
    private String StatusNotifyUserName;
    private RecommendInfo RecommendInfo;
    private int ForwardFlag;
    private AppInfo AppInfo;
    private int HasProductId;
    private String Ticket;
    private int ImgHeight;
    private int ImgWidth;
    private int SubMsgType;
    private long NewMsgId;
    private String OriContent;
    private String EncryFileName;


    /**
     * 自己添加的变量
     */
    private String mentionMeUserNickName;
    private boolean mentionMe;
    private boolean groupMsg;
    /**
     * 文本消息内容
     **/
    private String plainText;
    private WxRespConstant.WXReceiveMsgCodeEnum Type;
    private String memberName;
    /**
     * 文件地址
     */
    private String filePath;

    /**
     * 缩略图地址
     */
    private String slavePath;

    /**
     * content map
     */
    private Map<String, Object> contentMap;

}