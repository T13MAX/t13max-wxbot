package com.t13max.wxbot.manager;

import com.t13max.common.manager.ManagerBase;
import com.t13max.util.TimeUtil;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.utils.QRUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 机器人管理器
 *
 * @author t13max
 * @since 14:47 2024/12/16
 */
public class RobotManager extends ManagerBase {

    private final Map<String, Robot> robotMap = new ConcurrentHashMap<>();

    private final ExecutorService tickExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean stop;

    private final AtomicInteger ID = new AtomicInteger(10001);

    private long lastTickMills;

    public static RobotManager inst() {
        return inst(RobotManager.class);
    }

    @Override
    protected void onShutdown() {
        stop = true;
        tickExecutor.shutdown();
        try {
            if (!tickExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                tickExecutor.shutdownNow();
            }
        } catch (InterruptedException ignore) {

        }
    }

    @Override
    protected void init() {
        tickExecutor.execute(() -> {
            while (!stop) {
                long startTimeMillis = TimeUtil.nowMills();
                this.lastTickMills = startTimeMillis;
                robotMap.values().forEach(Robot::tick);
                long endTimeMillis = TimeUtil.nowMills();
                long intervalMills = endTimeMillis - startTimeMillis;
                try {
                    Thread.sleep(Math.max(200 - intervalMills, 1));
                } catch (InterruptedException ignore) {

                }
            }
        });
    }

    public Robot createRobot() {
        Robot robot = new Robot();
        this.robotMap.put(String.valueOf(ID.getAndIncrement()), robot);
        return robot;
    }
}
