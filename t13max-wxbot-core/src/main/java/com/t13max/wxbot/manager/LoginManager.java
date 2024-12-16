package com.t13max.wxbot.manager;

import com.t13max.common.manager.ManagerBase;
import com.t13max.wxbot.Robot;
import com.t13max.wxbot.consts.StorageLoginInfoEnum;
import com.t13max.wxbot.consts.WxReqParamsConstant;
import com.t13max.wxbot.consts.WxRespConstant;
import com.t13max.wxbot.consts.WxURLEnum;
import com.t13max.wxbot.dto.request.*;
import com.t13max.wxbot.dto.response.wxinit.WxInitResponse;
import com.t13max.wxbot.entity.Contacts;
import com.alibaba.fastjson.JSON;
import com.t13max.wxbot.exception.RobotException;
import com.t13max.wxbot.utils.*;
import lombok.extern.log4j.Log4j2;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 登录管理器
 *
 * @Author t13max
 * @Date 14:46 2024/12/16
 */
@Log4j2
public class LoginManager extends ManagerBase {

    public static LoginManager inst() {
        return inst(LoginManager.class);
    }

    /**
     * 检查登陆状态
     *
     * @Author t13max
     * @Date 15:03 2024/12/16
     */
    public WxRespConstant.CheckLoginResultCodeEnum checkQRCodeScanStatus(String result) {
        Matcher matcher = MatcherUtils.WINDOWS_CODE.matcher(result);
        if (matcher.find()) {
            return WxRespConstant.CheckLoginResultCodeEnum.getByCode(Integer.parseInt(matcher.group(1)));
        } else {
            throw new RobotException("获取二维码扫描状态码失败！");
        }
    }

    public static void main(String[] args) {
        String url = "window.redirect_uri=\"https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?ticket=A8XCLb3mURiL7HSW-Hwoqd3b@qrticket_0&uuid=wdhd2iiUGQ==&lang=zh_CN&scan=1685067009\"";
        Pattern pattern = Pattern.compile("(https?://[^/]+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String protocol = matcher.group(1);
            System.out.println("Protocol: " + protocol);
        }
    }

    /**
     * 处理登陆信息
     *
     * @Author t13max
     * @Date 15:04 2024/12/16
     */
    private String processQRScanInfo(Robot robot, String loginContent) throws Exception {
        Matcher matcher = MatcherUtils.WINDOW_REDIRECT_URI.matcher(loginContent);
        if (matcher.find()) {
            String originalUrl = matcher.group(1);
            String url = originalUrl.substring(0, originalUrl.lastIndexOf('/'));
            //获取主机名：https://wx2.qq.com/cgi-bin/mmwebwx-bin
            robot.getLoginResultData().setUrl(url);
            Map<String, List<String>> possibleUrlMap = this.getPossibleUrlMap();
            Iterator<Entry<String, List<String>>> iterator = possibleUrlMap.entrySet().iterator();
            Entry<String, List<String>> entry;
            String fileUrl;
            String syncUrl;
            while (iterator.hasNext()) {
                entry = iterator.next();
                String indexUrl = entry.getKey();
                fileUrl = "https://" + entry.getValue().get(0) + "/cgi-bin/mmwebwx-bin";
                syncUrl = "https://" + entry.getValue().get(1) + "/cgi-bin/mmwebwx-bin";
                if (robot.getLoginResultData().getUrl().contains(indexUrl)) {
                    robot.getLoginResultData().setFileUrl(fileUrl);
                    robot.getLoginResultData().setSyncUrl(syncUrl);
                    break;
                }
            }
            if (robot.getLoginResultData().getFileUrl() == null
                    && robot.getLoginResultData().getSyncUrl() == null) {
                robot.getLoginResultData().setFileUrl(url);
                robot.getLoginResultData().setSyncUrl(url);
            }
            robot.getLoginResultData().setDeviceId("e" + String.valueOf(new Random().nextLong()).substring(1, 16)); // 生成15位随机数
            robot.getLoginResultData().setBaseRequest(new BaseRequest());
            robot.getLoginResultData().getBaseRequest().setDeviceId(robot.getLoginResultData().getDeviceId());
            return originalUrl;
        }
        throw new RobotException("获取登录地址失败！");
    }

    /**
     * 准备登录
     *
     * @Author t13max
     * @Date 15:06 2024/12/16
     */
    public boolean preLogin(Robot robot) throws Exception {

        boolean isLogin = false;
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(WxReqParamsConstant.LoginParaEnum.LOGIN_ICON.para(), WxReqParamsConstant.LoginParaEnum.LOGIN_ICON.value()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.LoginParaEnum.UUID.para(), robot.getUuid()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.LoginParaEnum.TIP.para(), WxReqParamsConstant.LoginParaEnum.TIP.value()));

        while1:
        while (!isLogin) {

            long millis = System.currentTimeMillis();
            params.add(new BasicNameValuePair(WxReqParamsConstant.LoginParaEnum.R.para(), String.valueOf(millis / 1579L)));
            params.add(new BasicNameValuePair(WxReqParamsConstant.LoginParaEnum.UNDER_LINE.para(), String.valueOf(millis)));
            HttpEntity entity = HttpUtil.doGet(WxURLEnum.LOGIN_URL.getUrl(), params, true, null);

            try {
                String result = EntityUtils.toString(entity);
                WxRespConstant.CheckLoginResultCodeEnum codeEnum = checkQRCodeScanStatus(result);
                switch (codeEnum) {
                    case SUCCESS -> {
                        String redirectUrl = processQRScanInfo(robot, result);
                        doLogin(robot, redirectUrl);
                        isLogin = true;
                        robot.setAlive(true);
                        break while1;
                    }
                    case CANCEL, NONE, WAIT_SCAN, WAIT_CONFIRM -> {
                        log.info(codeEnum.getMsg());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("微信登陆异常：{}", e.getMessage());
            }
            Thread.sleep(100);
        }
        return isLogin;
    }

    /**
     * 登录
     *
     * @Author t13max
     * @Date 15:09 2024/12/16
     */
    public void doLogin(Robot robot, String redirectUrl) throws Exception {
        try {
            String url = redirectUrl + "&fun=new&version=v2&mod=desktop&lang=zh_CN";
            Map<String, String> header = new HashMap<>();
            //UOS header
            header.put("client-version", "2.0.0");
            header.put("extspam", "Go8FCIkFEokFCggwMDAwMDAwMRAGGvAESySibk50w5Wb3uTl2c2h64jVVrV7gNs06GFlWplHQbY/5FfiO++1yH4ykCyNPWKXmco+wfQzK5R98D3so7rJ5LmGFvBLjGceleySrc3SOf2Pc1gVehzJgODeS0lDL3/I/0S2SSE98YgKleq6Uqx6ndTy9yaL9qFxJL7eiA/R3SEfTaW1SBoSITIu+EEkXff+Pv8NHOk7N57rcGk1w0ZzRrQDkXTOXFN2iHYIzAAZPIOY45Lsh+A4slpgnDiaOvRtlQYCt97nmPLuTipOJ8Qc5pM7ZsOsAPPrCQL7nK0I7aPrFDF0q4ziUUKettzW8MrAaiVfmbD1/VkmLNVqqZVvBCtRblXb5FHmtS8FxnqCzYP4WFvz3T0TcrOqwLX1M/DQvcHaGGw0B0y4bZMs7lVScGBFxMj3vbFi2SRKbKhaitxHfYHAOAa0X7/MSS0RNAjdwoyGHeOepXOKY+h3iHeqCvgOH6LOifdHf/1aaZNwSkGotYnYScW8Yx63LnSwba7+hESrtPa/huRmB9KWvMCKbDThL/nne14hnL277EDCSocPu3rOSYjuB9gKSOdVmWsj9Dxb/iZIe+S6AiG29Esm+/eUacSba0k8wn5HhHg9d4tIcixrxveflc8vi2/wNQGVFNsGO6tB5WF0xf/plngOvQ1/ivGV/C1Qpdhzznh0ExAVJ6dwzNg7qIEBaw+BzTJTUuRcPk92Sn6QDn2Pu3mpONaEumacjW4w6ipPnPw+g2TfywJjeEcpSZaP4Q3YV5HG8D6UjWA4GSkBKculWpdCMadx0usMomsSS/74QgpYqcPkmamB4nVv1JxczYITIqItIKjD35IGKAUwAA==");

            HttpEntity entity = HttpUtil.doGet(url, null, false, header);
            String resultOfXml = EntityUtils.toString(entity);

            //如果登录被禁止时，则登录返回的message内容不为空，下面代码则判断登录内容是否为空，不为空则退出程序
            String msg = getLoginMessage(resultOfXml);
            if (!"".equals(msg)) {
                throw new Exception(msg);
            }
            //解析XML
            Document doc = CommonTools.xmlParser(resultOfXml);
            if (doc != null) {
                robot.getLoginResultData().getBaseRequest().setSKey(
                        doc.getElementsByTagName(StorageLoginInfoEnum.skey.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                robot.getLoginResultData().getBaseRequest().setWxSid(
                        doc.getElementsByTagName(StorageLoginInfoEnum.wxsid.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                robot.getLoginResultData().getBaseRequest().setWxUin(
                        doc.getElementsByTagName(StorageLoginInfoEnum.wxuin.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                robot.getLoginResultData().setPassTicket(
                        doc.getElementsByTagName(StorageLoginInfoEnum.pass_ticket.getKey()).item(0).getFirstChild()
                                .getNodeValue());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }


    }

    /**
     * 获取uuid
     *
     * @Author t13max
     * @Date 15:10 2024/12/16
     */
    public String getUuid(Robot robot) {
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(WxReqParamsConstant.UUIDParaEnum.APP_ID.para(), WxReqParamsConstant.UUIDParaEnum.APP_ID.value()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.UUIDParaEnum.FUN.para(), WxReqParamsConstant.UUIDParaEnum.FUN.value()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.UUIDParaEnum.LANG.para(), WxReqParamsConstant.UUIDParaEnum.LANG.value()));
        params.add(new BasicNameValuePair(WxReqParamsConstant.UUIDParaEnum.UNDER_LINE.para(), String.valueOf(System.currentTimeMillis())));

        HttpEntity entity = HttpUtil.doGet(WxURLEnum.UUID_URL.getUrl(), params, true, null);

        try {
            String result = EntityUtils.toString(entity);
            String regEx = "window.QRLogin.code = (\\d+); window.QRLogin.uuid = \"(\\S+?)\";";
            Matcher matcher = CommonTools.getMatcher(regEx, result);
            if (matcher.find()) {
                if (("200".equals(matcher.group(1)))) {
                    robot.setUuid(matcher.group(2));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return robot.getUuid();
    }

    /**
     * 获取二维码
     *
     * @Author t13max
     * @Date 15:10 2024/12/16
     */
    public boolean getQR(Robot robot, String qrPath) {
        String qrUrl = WxURLEnum.QRCODE_URL.getUrl() + robot.getUuid();
        HttpEntity entity = HttpUtil.doGet(qrUrl, null, true, null);
        try {
            //下载二维码图片
            OutputStream out = new FileOutputStream(qrPath);
            byte[] bytes = EntityUtils.toByteArray(entity);
            out.write(bytes);
            out.flush();
            out.close();
            //二维码地址
            String qrUrl2 = WxURLEnum.cAPI_qrcode.getUrl() + robot.getUuid();
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }

        return true;
    }

    public BufferedImage getQR(Robot robot) {
        String qrUrl = WxURLEnum.QRCODE_URL.getUrl() + robot.getUuid();
        HttpEntity entity = HttpUtil.doGet(qrUrl, null, true, null);
        try {
            BufferedImage image = ImageIO.read(entity.getContent());
            return image;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 登陆完初始化
     *
     * @Author t13max
     * @Date 15:10 2024/12/16
     */
    public boolean webWxInit(Robot robot) {
        robot.setAlive(true);
        robot.setLastNormalRetCodeTime(System.currentTimeMillis());
        // 组装请求URL和参数
        String url = String.format(WxURLEnum.INIT_URL.getUrl(),
                robot.getLoginResultData().getUrl(),
                System.currentTimeMillis() / 3158L,
                robot.getLoginResultData().getPassTicket());

        // 请求初始化接口
        WxInitReq wxInitReq = new WxInitReq();
        wxInitReq.setBaseRequest(robot.getLoginResultData().getBaseRequest());
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(wxInitReq));
        try {
            String result = EntityUtils.toString(entity, Consts.UTF_8);
            WxInitResponse wxInitResponse = JSON.parseObject(result, WxInitResponse.class);
            Contacts me = wxInitResponse.getUser();
            ;

            robot.getLoginResultData().setInviteStartCount(wxInitResponse.getInviteStartCount());
            robot.getLoginResultData().setSyncKeyObject(wxInitResponse.getSyncKey());


            robot.setUserName(me.getUsername());
            robot.setNickName(me.getNickname());
            robot.setUserSelf(me);
            robot.getMemberMap().put(me.getUsername(), me);
            //初始化列表的联系人
            //最近聊天的联系人

            Set<String> recentContacts = robot.getRecentContacts();
            for (Contacts contacts : wxInitResponse.getContactList()) {
                ContactsManager.inst().addContacts(robot, contacts);
                recentContacts.add(contacts.getUsername());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 状态通知
     *
     * @Author t13max
     * @Date 15:12 2024/12/16
     */
    public void wxStatusNotify(Robot robot) {
        // 组装请求URL和参数
        String url = String.format(WxURLEnum.STATUS_NOTIFY_URL.getUrl(),
                robot.getLoginResultData().getUrl(),
                robot.getLoginResultData().getPassTicket());

        WxStatusNotifyReq wxStatusNotifyReq = new WxStatusNotifyReq();
        wxStatusNotifyReq.setBaseRequest(robot.getLoginResultData().getBaseRequest());
        wxStatusNotifyReq.setCode(3);
        wxStatusNotifyReq.setFromUserName(robot.getUserName());
        wxStatusNotifyReq.setToUserName(robot.getUserName());
        wxStatusNotifyReq.setClientMsgId(System.currentTimeMillis());
        String paramStr = JSON.toJSONString(wxStatusNotifyReq);

        try {
            HttpEntity entity = HttpUtil.doPost(url, paramStr);
            EntityUtils.toString(entity, Consts.UTF_8);
        } catch (Exception e) {
            log.error("微信状态通知接口失败！", e);
        }

    }

    /**
     * 检查登录人的头像
     */
    public String getUserAvatar(String result) {
        String regEx = "window.userAvatar\\s*=\\s*'data:img/jpg;base64,(.+)'";
        Matcher matcher = CommonTools.getMatcher(regEx, result);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    private Map<String, List<String>> getPossibleUrlMap() {
        Map<String, List<String>> possibleUrlMap = new HashMap<String, List<String>>();
        possibleUrlMap.put("wx.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx.qq.com");
                add("webpush.wx.qq.com");
            }
        });

        possibleUrlMap.put("wx2.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx2.qq.com");
                add("webpush.wx2.qq.com");
            }
        });
        possibleUrlMap.put("wx8.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx8.qq.com");
                add("webpush.wx8.qq.com");
            }
        });

        possibleUrlMap.put("web2.wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web2.wechat.com");
                add("webpush.web2.wechat.com");
            }
        });
        possibleUrlMap.put("wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web.wechat.com");
                add("webpush.web.wechat.com");
            }
        });
        return possibleUrlMap;
    }

    /**
     * 解析登录返回的消息，如果成功登录，则message为空
     *
     * @Author t13max
     * @Date 15:17 2024/12/16
     */
    public String getLoginMessage(String result) {
        String[] strArr = result.split("<message>");
        String[] rs = strArr[1].split("</message>");
        if (rs.length > 1) {
            return rs[0];
        }
        return "";
    }

}
