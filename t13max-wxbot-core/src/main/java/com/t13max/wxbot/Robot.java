package com.t13max.wxbot;

import com.t13max.wxbot.consts.PhaseEnum;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.entity.LoginResultData;
import com.t13max.wxbot.entity.Message;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.manager.CallBackManager;
import com.t13max.wxbot.manager.ContactsManager;
import com.t13max.wxbot.manager.LoginManager;
import com.t13max.wxbot.manager.MessageManager;
import com.t13max.wxbot.tools.MessageTools;
import com.t13max.wxbot.utils.ExecutorServiceUtil;
import com.t13max.wxbot.utils.SleepUtils;
import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author t13max
 * @since 14:22 2024/12/16
 */
@Data
public class Robot implements IRobot {

    private volatile boolean alive = false;
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

    @Override
    public BufferedImage getQR() throws Exception {
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

        //优化!

        BufferedImage bufferedImage = LoginManager.inst().getQR(this);

        ExecutorServiceUtil.getGlobalExecutorService().execute(() -> {
            try {
                LoginManager.inst().preLogin(this);
                LoginManager.inst().webWxInit(this);
                ContactsManager.inst().webWxGetContact(this);
                MessageManager.inst().startReceiving(this);
                ContactsManager.inst().WebWxBatchGetContact(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return bufferedImage;
    }

    @Override
    public void register(MessageHandler messageHandler) {
        if (alive) {
            throw new RobotException("登录状态禁止注册监听");
        }
        MessageManager.inst().register(this, messageHandler);
    }

    @Override
    public void loginCallBack(ICallBack callBack) {
        CallBackManager.inst().addCallBack(this, PhaseEnum.LOGIN_SUCCESS, callBack);
    }

    @Override
    public void sendMsg(Message message) {
        MessageTools.sendMsgByUserId(this, message);
    }
}
