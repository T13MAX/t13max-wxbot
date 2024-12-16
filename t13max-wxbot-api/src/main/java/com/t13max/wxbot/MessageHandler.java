package com.t13max.wxbot;

import com.t13max.wxbot.entity.Message;

/**
 * 消息处理器
 *
 * @author t13max
 * @since 14:26 2024/12/16
 */
public interface MessageHandler {

    void handle(Message message);
}
