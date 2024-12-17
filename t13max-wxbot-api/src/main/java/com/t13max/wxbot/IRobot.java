package com.t13max.wxbot;

import com.t13max.wxbot.entity.Message;

import java.awt.image.BufferedImage;

/**
 * 机器人接口
 *
 * @author t13max
 * @since 14:21 2024/12/16
 */
public interface IRobot {

    //获取二维码
    BufferedImage getQR() throws Exception;

    //注册消息监听
    void register(MessageHandler messageHandler);

    //发送一个消息
    void sendMsg(Message message);
}
