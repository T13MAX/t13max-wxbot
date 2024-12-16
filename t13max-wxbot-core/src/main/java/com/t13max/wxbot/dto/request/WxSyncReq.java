package com.t13max.wxbot.dto.request;

import com.t13max.wxbot.dto.response.sync.SyncKey;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WxSyncReq {
    @JSONField(name = "BaseRequest")
    private BaseRequest BaseRequest;

    @JSONField(name = "SyncKey")
    private SyncKey SyncKey;

    @JSONField(name = "rr")
    private Long rr;
}
