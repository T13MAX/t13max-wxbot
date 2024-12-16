package com.t13max.wxbot.manager;

import com.t13max.common.manager.ManagerBase;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.utils.QRUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 机器人管理器
 *
 * @author t13max
 * @since 14:47 2024/12/16
 */
public class RobotManager extends ManagerBase {

    public static RobotManager inst() {
        return inst(RobotManager.class);
    }

    public Robot createRobot() {
        return new Robot();
    }

    public static void main(String[] args) throws Exception {
        Robot robot = RobotManager.inst().createRobot();
        BufferedImage bufferedImage = robot.getQR();
        Frame frame = QRUtils.createFrame(bufferedImage);
    }
}
