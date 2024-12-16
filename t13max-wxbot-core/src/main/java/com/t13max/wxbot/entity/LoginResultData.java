package com.t13max.wxbot.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.t13max.wxbot.dto.request.BaseRequest;
import com.t13max.wxbot.dto.response.sync.SyncCheckKey;
import com.t13max.wxbot.dto.response.sync.SyncKey;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

@Getter
@Setter
public class LoginResultData {
        private String url;
        private String fileUrl;
        private String syncUrl;
        private String deviceId;
        private Integer inviteStartCount;
        private SyncKey syncKeyObject;
        private String syncKey;
        private SyncCheckKey syncCheckKey;
        @JSONField(name ="pass_ticket")
        private String passTicket;
        @JSONField(name ="BaseRequest")
        private BaseRequest baseRequest;



    public void setSyncKeyObject(SyncKey syncKeyObject) {
        this.syncKey = syncKeyObject.getList()
                .stream()
                .map(e -> e.getKey() + "_" + e.getVal()).collect(Collectors.joining("|"));
        this.syncKeyObject = syncKeyObject;
    }
}