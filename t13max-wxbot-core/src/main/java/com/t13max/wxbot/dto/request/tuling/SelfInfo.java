/**
 * Copyright 2021 bejson.com
 */
package com.t13max.wxbot.dto.request.tuling;

import lombok.Builder;

/**
 * Auto-generated: 2021-02-02 14:39:21
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Builder
public class SelfInfo {

    private Location location;

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

}