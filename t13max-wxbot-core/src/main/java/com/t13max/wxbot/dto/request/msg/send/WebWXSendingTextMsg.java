package com.t13max.wxbot.dto.request.msg.send;

import com.t13max.wxbot.consts.WxReqParamsConstant;

/**
 * @作者 舒新胜
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * 文本消息
 */

public class WebWXSendingTextMsg extends WebWXSendingMsg {

    public WebWXSendingTextMsg() {
        super(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode());
    }
}
