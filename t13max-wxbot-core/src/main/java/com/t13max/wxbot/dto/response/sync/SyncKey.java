/**
 * Copyright 2021 bejson.com
 */
package com.t13max.wxbot.dto.response.sync;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * Auto-generated: 2021-02-22 13:35:59
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class SyncKey {

    @JSONField(name="Count")
    private int Count;

    @JSONField(name="List")
    private List<com.t13max.wxbot.dto.response.sync.List> List;


}