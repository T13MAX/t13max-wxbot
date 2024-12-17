package com.t13max.wxbot;

import com.t13max.common.run.Application;
import com.t13max.common.run.ConfigClazz;
import com.t13max.wxbot.consts.RobotConfig;

/**
 * @author t13max
 * @since 13:45 2024/12/17
 */
@ConfigClazz(configClazz=RobotConfig.class)
public class RobotApplication {

    public static void main(String[] args) throws Exception {
        Application.run(RobotApplication.class, args);
    }
}
