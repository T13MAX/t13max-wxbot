package com.t13max.wxbot.dto.response;

import com.t13max.wxbot.entity.Contacts;
import lombok.Data;

import java.util.List;

@Data
public class WxCreateRoomResp {

    private BaseResponse BaseResponse;

    private String Topic;


    private String PYInitial;


    private String QuanPin;


    private Integer MemberCount;


    private List<Contacts> MemberList;


    private String ChatRoomName;


    private String BlackList;
}
