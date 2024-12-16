package com.t13max.wxbot.utils;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

/**
 * 休眠工具类
 *
 * @Author t13max
 * @Date 16:15 2024/12/16
 */
@Log4j2
public class SleepUtils {

    /**
     * 毫秒为单位
     *
     * @param time 休眠时间 毫秒
     */
    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
