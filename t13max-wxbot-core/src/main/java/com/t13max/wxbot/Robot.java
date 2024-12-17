package com.t13max.wxbot;

import com.t13max.common.event.GameEventBus;
import com.t13max.wxbot.consts.RobotStatusEnum;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.entity.LoginResultData;
import com.t13max.wxbot.entity.Message;
import com.t13max.wxbot.event.LoginSuccessEvent;
import com.t13max.wxbot.event.QrSuccessEvent;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.manager.ContactsManager;
import com.t13max.wxbot.manager.LoginManager;
import com.t13max.wxbot.manager.MessageManager;
import com.t13max.wxbot.tools.MessageTools;
import com.t13max.wxbot.utils.SleepUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author t13max
 * @since 14:22 2024/12/16
 */
@Getter
@Setter
public class Robot implements IRobot {

    private volatile RobotStatusEnum statusEnum = RobotStatusEnum.IDLE;
    private String uuid = null;
    private String userName;
    private String nickName;
    private Contacts userSelf;
    private final Map<String, Contacts> memberMap = new ConcurrentHashMap<>(1024);
    private final Map<String, Contacts> contactMap = new ConcurrentHashMap<>(1024);
    private final Map<String, Contacts> groupMap = new ConcurrentHashMap<>(32);
    private final Map<String, Contacts> publicUsersMap = new ConcurrentHashMap<>(64);
    private final Set<String> recentContacts = new CopyOnWriteArraySet<>();
    private final Set<String> groupIdSet = new CopyOnWriteArraySet<>();

    //消息同步失败重试次数
    private int receivingRetryCount = 5;
    //最后一次收到正常code的时间，秒为单位
    private long lastNormalRetCodeTime;
    //登录结果
    private LoginResultData loginResultData = new LoginResultData();

    public Robot() {
    }

    public void tick() {

        try {
            doTick();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }

    private void doTick() throws Exception {
        switch (statusEnum) {
            case IDLE -> {
                //刚创建完 空闲状态 啥也不干
            }
            case QR -> {
                //尝试获取二维码
            }
            case PRE_LOGIN -> {
                //获取了二维码 等待扫描
                LoginManager.inst().preLogin(this);
                changeStatus(RobotStatusEnum.LOGIN);
                GameEventBus.inst().postEvent(new QrSuccessEvent());
            }
            case LOGIN -> {
                LoginManager.inst().webWxInit(this);
                ContactsManager.inst().webWxGetContact(this);
                ContactsManager.inst().WebWxBatchGetContact(this);
                changeStatus(RobotStatusEnum.RUNNING);
                GameEventBus.inst().postEvent(new LoginSuccessEvent());
            }
            case RUNNING -> {
                MessageManager.inst().startReceiving(this);
            }
        }
    }

    @Override
    public BufferedImage getQR() {
        int count = 0;
        while (count++ < 100) {
            String uuid = LoginManager.inst().getUuid(this);
            if (uuid != null) {
                break;
            }
            SleepUtils.sleep(2000);
        }
        if (this.uuid == null) {
            return null;
        }

        BufferedImage bufferedImage = LoginManager.inst().getQR(this);

        changeStatus(RobotStatusEnum.PRE_LOGIN);

        return bufferedImage;
    }

    @Override
    public void register(MessageHandler messageHandler) {
        MessageManager.inst().register(this, messageHandler);
    }

    @Override
    public void sendMsg(Message message) {
        MessageTools.sendMsgByUserId(this, message);
    }

    public void changeStatus(RobotStatusEnum statusEnum) {
        this.statusEnum = statusEnum;
    }
}
