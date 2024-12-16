package com.t13max.wxbot.dto.request.msg.send;

import com.t13max.wxbot.consts.WxReqParamsConstant;

/**
 * @作者 舒新胜
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * 图片消息
 */

public class WebWXSendingEmotionMsg extends WebWXSendingMsg {
    public Integer EmojiFlag = null;
    public String EMoticonMd5;

    public WebWXSendingEmotionMsg() {
        super(WxReqParamsConstant.WXSendMsgCodeEnum.EMOTION.getCode());
        super.Content = null;
    }
}
