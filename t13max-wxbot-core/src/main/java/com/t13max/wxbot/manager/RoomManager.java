package com.t13max.wxbot.manager;

import com.alibaba.fastjson.JSON;
import com.t13max.common.manager.ManagerBase;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.dto.request.WxCreateRoomReq;
import com.t13max.wxbot.dto.response.WxCreateRoomResp;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.utils.HttpUtil;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 群聊管理器
 *
 * @author t13max
 * @since 15:18 2024/12/16
 */
public class RoomManager extends ManagerBase {

    public static RoomManager inst() {
        return inst(RoomManager.class);
    }

    public WxCreateRoomResp webWxCreateRoom(Robot robot, List<Contacts> contacts) throws Exception {
        if (contacts.isEmpty()) {
            throw new RobotException("contacts is empty.");
        }

        String url = String.format(WxURLEnum.WEB_WX_CREATE_ROOM.getUrl(),
                robot.getLoginResultData().getUrl(),
                System.currentTimeMillis());
        WxCreateRoomReq createRoomReq = WxCreateRoomReq.builder().BaseRequest(robot.getLoginResultData().getBaseRequest())
                .MemberCount(contacts.size())
                .MemberList(contacts.stream()
                        .map(e -> WxCreateRoomReq.NewRoomMember
                                .builder()
                                .UserName(e.getUsername()).build())
                        .collect(Collectors.toList()))
                .Topic("")
                .build();
        HttpEntity httpEntity = HttpUtil.doPost(url, JSON.toJSONString(createRoomReq));
        return JSON.parseObject(EntityUtils.toString(httpEntity, StandardCharsets.UTF_8), WxCreateRoomResp.class);
    }
}
