package com.t13max.wxbot.event;

import com.t13max.common.event.IEvent;
import com.t13max.common.event.IEventEnum;
import com.t13max.wxbot.consts.RobotEventEnum;

/**
 * @author t13max
 * @since 11:24 2024/12/17
 */
public class QrSuccessEvent implements IEvent {

    @Override
    public IEventEnum getEventEnum() {
        return RobotEventEnum.QR_SUCCESS;
    }
}
