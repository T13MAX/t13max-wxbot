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
public class SyncCheckKey {

    private int Count;
    private List<com.t13max.wxbot.dto.response.sync.List> List;

    @JSONField(name = "Count")
    public int getCount() {
        return Count;
    }

    @JSONField(name = "List")
    public java.util.List<com.t13max.wxbot.dto.response.sync.List> getList() {
        return List;
    }

    @JSONField(name = "Count")
    public void setCount(int count) {
        Count = count;
    }

    @JSONField(name = "List")
    public void setList(java.util.List<com.t13max.wxbot.dto.response.sync.List> list) {
        List = list;
    }
}