package com.t13max.wxbot.manager;

import com.t13max.common.manager.ManagerBase;
import com.t13max.wxbot.ICallBack;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.PhaseEnum;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 回调管理器
 *
 * @author t13max
 * @since 14:51 2024/12/16
 */
public class CallBackManager extends ManagerBase {

    private Map<Robot, Map<PhaseEnum, List<ICallBack>>> callBackMap = new ConcurrentHashMap<>();

    public static CallBackManager inst() {
        return inst(CallBackManager.class);
    }

    public void addCallBack(Robot robot, PhaseEnum phaseEnum, ICallBack callBack) {
        Map<PhaseEnum, List<ICallBack>> phaseCallBackMap = callBackMap.computeIfAbsent(robot, k -> new ConcurrentHashMap<>());
        List<ICallBack> callBackList = phaseCallBackMap.computeIfAbsent(phaseEnum, k -> new CopyOnWriteArrayList<>());
        callBackList.add(callBack);
    }

    public void trigger(Robot robot, PhaseEnum phaseEnum) {
        Map<PhaseEnum, List<ICallBack>> phaseCallBackMap = callBackMap.get(robot);
        if (phaseCallBackMap == null) {
            return;
        }
        List<ICallBack> callBackList = phaseCallBackMap.get(phaseEnum);
        for (ICallBack callBack : callBackList) {
            callBack.callBack();
        }
    }
}
