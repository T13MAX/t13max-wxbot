package com.t13max.wxbot.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.t13max.common.manager.ManagerBase;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.StorageLoginInfoEnum;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.entity.Contacts;
import com.t13max.wxbot.utils.HttpUtil;
import com.t13max.wxbot.utils.Log;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.*;

/**
 * 联系人管理器
 *
 * @author t13max
 * @since 15:15 2024/12/16
 */
public class ContactsManager extends ManagerBase {

    public static ContactsManager inst() {
        return inst(ContactsManager.class);
    }

    public void webWxGetContact(Robot robot) {
        String url = String.format(WxURLEnum.WEB_WX_GET_CONTACT.getUrl(),
                robot.getLoginResultData().getUrl());
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(robot.getLoginResultData().getBaseRequest()));
        if (entity == null) {
            return;
        }
        try {
            String result = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject fullFriendsJsonList = JSON.parseObject(result);
            // 查看seq是否为0，0表示好友列表已全部获取完毕，若大于0，则表示好友列表未获取完毕，当前的字节数（断点续传）
            long seq = 0;
            long currentTime = 0L;
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            if (fullFriendsJsonList.get("Seq") != null) {
                seq = fullFriendsJsonList.getLong("Seq");
                currentTime = System.currentTimeMillis();
            }
            JSONArray member = fullFriendsJsonList.getJSONArray(StorageLoginInfoEnum.MemberList.getKey());
            // 循环获取seq直到为0，即获取全部好友列表
            // ==0：好友获取完毕
            // >0：好友未获取完毕，此时seq为已获取的字节数
            while (seq > 0) {
                // 设置seq传参
                params.add(new BasicNameValuePair("r", String.valueOf(currentTime)));
                params.add(new BasicNameValuePair("seq", String.valueOf(seq)));
                entity = HttpUtil.doGet(url, params, false, null);

                params.remove(new BasicNameValuePair("r", String.valueOf(currentTime)));
                params.remove(new BasicNameValuePair("seq", String.valueOf(seq)));

                result = EntityUtils.toString(entity, Consts.UTF_8);
                fullFriendsJsonList = JSON.parseObject(result);

                if (fullFriendsJsonList.get("Seq") != null) {
                    seq = fullFriendsJsonList.getLong("Seq");
                    currentTime = System.currentTimeMillis();
                }

                // 累加好友列表
                member.addAll(fullFriendsJsonList.getJSONArray(StorageLoginInfoEnum.MemberList.getKey()));
            }
            for (Object value : member) {
                JSONObject o = (JSONObject) value;
                Contacts contacts = JSON.parseObject(JSON.toJSONString(o), Contacts.class);
                addContacts(robot, contacts);
            }
            if (!robot.getMemberMap().containsKey("filehelper")) {
                robot.getMemberMap().put("filehelper",
                        Contacts.builder().username("filehelper").displayname("文件传输助手")
                                .type(Contacts.ContactsType.ORDINARY_USER).build());
            }

        } catch (Exception e) {
            Log.DEF.error(e.getMessage(), e);
        }
    }


    /**
     * 添加联系人
     */
    public void addContacts(Robot robot, Contacts contacts) {

        String userName = contacts.getUsername();
        String nickName = contacts.getNickname();
        //保存之前的群信息 方便compare
        if (robot.getMemberMap().containsKey(contacts.getUsername())) {
            contacts.setMemberlist(robot.getMemberMap().get(contacts.getUsername()).getMemberlist());
        }
        robot.getMemberMap().put(userName, contacts);

        if ((contacts.getVerifyflag() & 8) != 0) {
            // 公众号/服务号
            if (!robot.getPublicUsersMap().containsKey(userName)) {
                Log.DEF.info("新增公众号/服务号：{}", nickName);
            }
            robot.getPublicUsersMap().put(userName, contacts);
            contacts.setType(Contacts.ContactsType.PUBLIC_USER);
        } else if (userName.startsWith("@@")) {
            // 群聊
            if (!robot.getGroupIdSet().contains(userName)) {
                Log.DEF.info("新增群聊：{}", nickName);
                robot.getGroupIdSet().add(userName);
            }
            contacts.setType(Contacts.ContactsType.GROUP_USER);
        } else {
            contacts.setType(Contacts.ContactsType.ORDINARY_USER);
            //比较上次差异
            /*if (compare) {
                Contacts old = robot.getContactMap().get(userName);
                ContactsTools.compareContacts(old, contacts);
            }*/

            // 普通联系人
            robot.getContactMap().put(userName, contacts);
        }
    }

    public void WebWxBatchGetContact(Robot robot) {
        String url = String.format(WxURLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                robot.getLoginResultData().getUrl(), new Date().getTime(),
                robot.getLoginResultData().getPassTicket());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("Count", robot.getGroupIdSet().size());
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(robot.getGroupIdSet().size());
        for (String s : robot.getGroupIdSet()) {
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("UserName", s);
            map.put("EncryChatRoomId", "");
            list.add(map);
        }
        paramMap.put("List", list);
        paramMap.put("BaseRequest", robot.getLoginResultData().getBaseRequest());
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        try {
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            //群列表
            JSONArray contactList = obj.getJSONArray("ContactList");
            for (int i = 0; i < contactList.size(); i++) {
                // 群好友
                JSONObject groupObject = contactList.getJSONObject(i);
                Contacts group = JSON.parseObject(JSON.toJSONString(groupObject), Contacts.class);
                String userName = group.getUsername();
                if (userName.startsWith("@@")) {
                    //以上接口返回的成员属性不全，以下的接口获取群成员详细属性
                    JSONArray memberArray = WebWxBatchGetContactDetail(robot, group);
                    List<Contacts> memberList = JSON.parseArray(JSON.toJSONString(memberArray), Contacts.class);
                    group.setMemberlist(memberList);

                    //比较群成员信息
                    Contacts old = robot.getGroupMap().get(userName);
                    //比较上次差异
                    //ContactsTools.compareGroup(old, group);

                    robot.getMemberMap().put(userName, group);
                    robot.getGroupMap().put(userName, group);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.DEF.error(e.getMessage());
        }
    }

    public List<Contacts> WebWxBatchGetContact(Robot robot, String groupName) {

        Log.DEF.info("加载群成员开始：" + groupName);
        String url = String.format(WxURLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                robot.getLoginResultData().getUrl(), new Date().getTime(),
                robot.getLoginResultData().getPassTicket());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("Count", 1);
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(1);
        HashMap<String, String> map = new HashMap<String, String>(2);
        map.put("UserName", groupName);
        map.put("EncryChatRoomId", "");
        list.add(map);
        paramMap.put("List", list);
        paramMap.put("BaseRequest", robot.getLoginResultData().getBaseRequest());
        HttpEntity entity = null;
        synchronized ((groupName + "WebWxBatchGetContact").intern()) {
            entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        }
        try {
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            //群列表
            JSONArray contactList = obj.getJSONArray("ContactList");
            for (int i = 0; i < contactList.size(); i++) {
                // 群好友
                JSONObject groupObject = contactList.getJSONObject(i);
                Contacts group = JSON.parseObject(JSON.toJSONString(groupObject), Contacts.class);
                group.setType(Contacts.ContactsType.GROUP_USER);
                String userName = group.getUsername();
                robot.getMemberMap().put(userName, group);
                if (userName.startsWith("@@")) {
                    //以上接口返回的成员属性不全，以下的接口获取群成员详细属性
                    JSONArray memberArray = WebWxBatchGetContactDetail(robot, group);
                    List<Contacts> memberList = JSON.parseArray(JSON.toJSONString(memberArray), Contacts.class);
                    group.setMemberlist(memberList);
                    robot.getGroupMap().put(userName, group);
                    robot.getMemberMap().put(userName, group);
                    Log.DEF.info("加载群成员结束：" + robot.getMemberMap().get(groupName).getMemberlist().size());
                    return memberList;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.DEF.error(e.getMessage());
        }
        Log.DEF.info("加载群成员结束：0");
        return new ArrayList<>();
    }

    public JSONArray WebWxBatchGetContactDetail(Robot robot, Contacts group) {
        String url = String.format(WxURLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                robot.getLoginResultData().getUrl(), System.currentTimeMillis(),
                robot.getLoginResultData().getPassTicket());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("BaseRequest", robot.getLoginResultData().getBaseRequest());
        //保存获取的群成员详细信息
        ArrayList<Contacts> groupContactsList = new ArrayList<>();
        JSONArray memberArray = new JSONArray();
        //保存需要获取详细资料的群成员username
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(group.getMemberlist().size());
        for (Contacts o : group.getMemberlist()) {
            //遍历群成员
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("UserName", o.getUsername());
            map.put("EncryChatRoomId", group.getUsername());
            list.add(map);
        }
        if (list.isEmpty()) {
            return memberArray;
        }
        //每次请求50个
        int ceil = (int) (Math.ceil((list.size() / 50.0)));

        for (int i = 0; i < ceil; i++) {
            List<Map<String, String>> subList = null;
            if (i < ceil - 1) {
                subList = list.subList(i * 50, i * 50 + 50);
            } else {
                subList = list.subList(i * 50, list.size());
            }
            paramMap.put("Count", subList.size());
            paramMap.put("List", subList);

            HttpEntity entity = null;
            synchronized ((group.getUsername() + "WebWxBatchGetContact").intern()) {
                entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
            }
            try {
                String text = EntityUtils.toString(entity, Consts.UTF_8);
                JSONObject obj = JSON.parseObject(text);
                JSONArray contactListArray = obj.getJSONArray("ContactList");
                memberArray.addAll(contactListArray);
            } catch (Exception e) {
                Log.DEF.error(e.getMessage());
            }
        }
        return memberArray;

    }

    public void handleModContact(Robot robot, List<Contacts> modContactList) {
        if (modContactList != null && !modContactList.isEmpty()) {
            for (Contacts contacts : modContactList) {
                handleModContact(robot, contacts);
            }
        }
    }

    public void handleModContact(Robot robot, Contacts contacts) {
        Log.DEF.info("联系人修改");
        if (contacts != null) {
            robot.getMemberMap().put(contacts.getUsername(), contacts);
        }

    }
}
