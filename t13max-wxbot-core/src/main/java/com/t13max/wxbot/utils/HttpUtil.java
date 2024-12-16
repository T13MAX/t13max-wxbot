package com.t13max.wxbot.utils;

import com.t13max.wxbot.consts.UOSConst;
import lombok.extern.log4j.Log4j2;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * HTTP访问类，对Apache HttpClient进行简单封装，适配器模式
 *
 * @author ShuXinSheng
 * @version 1.1
 * @date 创建时间：2017年4月9日 下午7:05:04
 */
@Log4j2
public class HttpUtil {

    /**
     * 用于接收文件或者其它类型的client 无超时时间（例如下载大文件）
     */
    private static final CloseableHttpClient myHttpClient;
    /**
     * 循环接收消息的client 有超时时间，避免卡住
     */
    private static final CloseableHttpClient receiveHttpClient;

    public static final CookieStore cookieStore;

    static {
        cookieStore = new BasicCookieStore();

        // 将CookieStore设置到MyHttpClient中
        myHttpClient = HttpClients.custom().setDefaultCookieStore(cookieStore)
                .build();


        //循环接收消息，需要设置超时时间，否则可能卡住
        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
        connManager.setSocketConfig(SocketConfig.custom().setSoTimeout(30000).build());
        receiveHttpClient = HttpClients.custom().setDefaultCookieStore(cookieStore)
                .setConnectionManager(connManager)
                .setConnectionTimeToLive(30000L, TimeUnit.MILLISECONDS)
                .build();
    }

    public static String getCookie(String name) {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equalsIgnoreCase(name)) {
                return cookie.getValue();
            }
        }
        return null;

    }

    private HttpUtil() {

    }

    /**
     * 获取cookies
     *
     * @author SXS
     * @date 2017年5月7日 下午8:37:17
     * @return
     */
/*	public static MyHttpClient getInstance() {
		if (instance == null) {
			synchronized (MyHttpClient.class) {
				if (instance == null) {
					instance = new MyHttpClient();
				}
			}
		}
		return instance;
	}*/

    /**
     * 处理GET请求
     *
     * @param url
     * @param params
     * @return
     * @author SXS
     * @date 2017年4月9日 下午7:06:19
     */
    public static HttpEntity doGetOfReceive(String url, List<BasicNameValuePair> params, boolean redirect,
                                            Map<String, String> headerMap) {
        HttpEntity entity = null;
        HttpGet httpGet;

        try {
            if (params != null) {
                String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
                httpGet = new HttpGet(url + "?" + paramStr);
            } else {
                httpGet = new HttpGet(url);
            }
            if (!redirect) {
                httpGet.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build()); // 禁止重定向
            }
            httpGet.setHeader("User-Agent", UOSConst.USER_AGENT);

            httpGet.setHeader("client-version", UOSConst.UOS_PATCH_CLIENT_VERSION);
            httpGet.setHeader("extspam", UOSConst.UOS_PATCH_EXTSPAM);
            httpGet.setHeader("referer", UOSConst.REFERER);


            if (headerMap != null) {
                Set<Entry<String, String>> entries = headerMap.entrySet();
                for (Entry<String, String> entry : entries) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            CloseableHttpResponse response = receiveHttpClient.execute(httpGet);
            entity = response.getEntity();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return entity;
    }

    /**
     * 处理GET请求
     *
     * @param url
     * @param params
     * @return
     * @author SXS
     * @date 2017年4月9日 下午7:06:19
     */
    public static HttpEntity doGet(String url, List<BasicNameValuePair> params, boolean redirect,
                                   Map<String, String> headerMap) {
        HttpEntity entity = null;
        HttpGet httpGet;

        try {
            if (params != null) {
                String paramStr = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
                httpGet = new HttpGet(url + "?" + paramStr);
            } else {
                httpGet = new HttpGet(url);
            }
            if (!redirect) {
                httpGet.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build()); // 禁止重定向
            }
            // httpGet.setHeader("client-version", "2.0.0");
            //httpGet.setHeader("extspam", "Gp8ICJkIEpkICggwMDAwMDAwMRAGGoAI1GiJSIpeO1RZTq9QBKsRbPJdi84ropi16EYI10WB6g74sGmRwSNXjPQnYUKYotKkvLGpshucCaeWZMOylnc6o2AgDX9grhQQx7fm2DJRTyuNhUlwmEoWhjoG3F0ySAWUsEbH3bJMsEBwoB//0qmFJob74ffdaslqL+IrSy7LJ76/G5TkvNC+J0VQkpH1u3iJJs0uUYyLDzdBIQ6Ogd8LDQ3VKnJLm4g/uDLe+G7zzzkOPzCjXL+70naaQ9medzqmh+/SmaQ6uFWLDQLcRln++wBwoEibNpG4uOJvqXy+ql50DjlNchSuqLmeadFoo9/mDT0q3G7o/80P15ostktjb7h9bfNc+nZVSnUEJXbCjTeqS5UYuxn+HTS5nZsPVxJA2O5GdKCYK4x8lTTKShRstqPfbQpplfllx2fwXcSljuYi3YipPyS3GCAqf5A7aYYwJ7AvGqUiR2SsVQ9Nbp8MGHET1GxhifC692APj6SJxZD3i1drSYZPMMsS9rKAJTGz2FEupohtpf2tgXm6c16nDk/cw+C7K7me5j5PLHv55DFCS84b06AytZPdkFZLj7FHOkcFGJXitHkX5cgww7vuf6F3p0yM/W73SoXTx6GX4G6Hg2rYx3O/9VU2Uq8lvURB4qIbD9XQpzmyiFMaytMnqxcZJcoXCtfkTJ6pI7a92JpRUvdSitg967VUDUAQnCXCM/m0snRkR9LtoXAO1FUGpwlp1EfIdCZFPKNnXMeqev0j9W9ZrkEs9ZWcUEexSj5z+dKYQBhIICviYUQHVqBTZSNy22PlUIeDeIs11j7q4t8rD8LPvzAKWVqXE+5lS1JPZkjg4y5hfX1Dod3t96clFfwsvDP6xBSe1NBcoKbkyGxYK0UvPGtKQEE0Se2zAymYDv41klYE9s+rxp8e94/H8XhrL9oGm8KWb2RmYnAE7ry9gd6e8ZuBRIsISlJAE/e8y8xFmP031S6Lnaet6YXPsFpuFsdQs535IjcFd75hh6DNMBYhSfjv456cvhsb99+fRw/KVZLC3yzNSCbLSyo9d9BI45Plma6V8akURQA/qsaAzU0VyTIqZJkPDTzhuCl92vD2AD/QOhx6iwRSVPAxcRFZcWjgc2wCKh+uCYkTVbNQpB9B90YlNmI3fWTuUOUjwOzQRxJZj11NsimjOJ50qQwTTFj6qQvQ1a/I+MkTx5UO+yNHl718JWcR3AXGmv/aa9rD1eNP8ioTGlOZwPgmr2sor2iBpKTOrB83QgZXP+xRYkb4zVC+LoAXEoIa1+zArywlgREer7DLePukkU6wHTkuSaF+ge5Of1bXuU4i938WJHj0t3D8uQxkJvoFi/EYN/7u2P1zGRLV4dHVUsZMGCCtnO6BBigFMAA=");
            // httpGet.setHeader("referer", "https://wx.qq.com/?&lang=zh_CN&target=t");

            // httpGet.setHeader("User-Agent", config.getUserAgent());
            if (headerMap != null) {
                Set<Entry<String, String>> entries = headerMap.entrySet();
                for (Entry<String, String> entry : entries) {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
            CloseableHttpResponse response = myHttpClient.execute(httpGet);
            entity = response.getEntity();
        } catch (IOException e) {
            // log.error(e.getMessage());
        }

        return entity;
    }

    /**
     * 处理POST请求
     *
     * @param url
     * @param paramsStr
     * @return
     * @author SXS
     * @date 2017年4月9日 下午7:06:35
     */
    public static HttpEntity doPost(String url, String paramsStr) {
        HttpEntity entity = null;
        HttpPost httpPost;
        try {
            StringEntity params = new StringEntity(paramsStr, Consts.UTF_8);
            httpPost = new HttpPost(url);
            httpPost.setEntity(params);
            httpPost.setHeader("Content-type", "application/json; charset=utf-8");
            httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36");

            httpPost.setHeader("client-version", "2.0.0");
            httpPost.setHeader("extspam", "Gp8ICJkIEpkICggwMDAwMDAwMRAGGoAI1GiJSIpeO1RZTq9QBKsRbPJdi84ropi16EYI10WB6g74sGmRwSNXjPQnYUKYotKkvLGpshucCaeWZMOylnc6o2AgDX9grhQQx7fm2DJRTyuNhUlwmEoWhjoG3F0ySAWUsEbH3bJMsEBwoB//0qmFJob74ffdaslqL+IrSy7LJ76/G5TkvNC+J0VQkpH1u3iJJs0uUYyLDzdBIQ6Ogd8LDQ3VKnJLm4g/uDLe+G7zzzkOPzCjXL+70naaQ9medzqmh+/SmaQ6uFWLDQLcRln++wBwoEibNpG4uOJvqXy+ql50DjlNchSuqLmeadFoo9/mDT0q3G7o/80P15ostktjb7h9bfNc+nZVSnUEJXbCjTeqS5UYuxn+HTS5nZsPVxJA2O5GdKCYK4x8lTTKShRstqPfbQpplfllx2fwXcSljuYi3YipPyS3GCAqf5A7aYYwJ7AvGqUiR2SsVQ9Nbp8MGHET1GxhifC692APj6SJxZD3i1drSYZPMMsS9rKAJTGz2FEupohtpf2tgXm6c16nDk/cw+C7K7me5j5PLHv55DFCS84b06AytZPdkFZLj7FHOkcFGJXitHkX5cgww7vuf6F3p0yM/W73SoXTx6GX4G6Hg2rYx3O/9VU2Uq8lvURB4qIbD9XQpzmyiFMaytMnqxcZJcoXCtfkTJ6pI7a92JpRUvdSitg967VUDUAQnCXCM/m0snRkR9LtoXAO1FUGpwlp1EfIdCZFPKNnXMeqev0j9W9ZrkEs9ZWcUEexSj5z+dKYQBhIICviYUQHVqBTZSNy22PlUIeDeIs11j7q4t8rD8LPvzAKWVqXE+5lS1JPZkjg4y5hfX1Dod3t96clFfwsvDP6xBSe1NBcoKbkyGxYK0UvPGtKQEE0Se2zAymYDv41klYE9s+rxp8e94/H8XhrL9oGm8KWb2RmYnAE7ry9gd6e8ZuBRIsISlJAE/e8y8xFmP031S6Lnaet6YXPsFpuFsdQs535IjcFd75hh6DNMBYhSfjv456cvhsb99+fRw/KVZLC3yzNSCbLSyo9d9BI45Plma6V8akURQA/qsaAzU0VyTIqZJkPDTzhuCl92vD2AD/QOhx6iwRSVPAxcRFZcWjgc2wCKh+uCYkTVbNQpB9B90YlNmI3fWTuUOUjwOzQRxJZj11NsimjOJ50qQwTTFj6qQvQ1a/I+MkTx5UO+yNHl718JWcR3AXGmv/aa9rD1eNP8ioTGlOZwPgmr2sor2iBpKTOrB83QgZXP+xRYkb4zVC+LoAXEoIa1+zArywlgREer7DLePukkU6wHTkuSaF+ge5Of1bXuU4i938WJHj0t3D8uQxkJvoFi/EYN/7u2P1zGRLV4dHVUsZMGCCtnO6BBigFMAA=");
            httpPost.setHeader("referer", "https://wx.qq.com/?&lang=zh_CN&target=t");

            CloseableHttpResponse response = myHttpClient.execute(httpPost);
            entity = response.getEntity();

        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return entity;
    }

    /**
     * 上传文件到服务器
     *
     * @param url
     * @param reqEntity
     * @return
     * @author SXS
     * @date 2017年5月7日 下午9:19:23
     */
    public static HttpEntity doPostFile(String url, HttpEntity reqEntity) {
        HttpEntity entity = null;
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36");


        httpPost.setHeader("client-version", "2.0.0");
        httpPost.setHeader("extspam", "Gp8ICJkIEpkICggwMDAwMDAwMRAGGoAI1GiJSIpeO1RZTq9QBKsRbPJdi84ropi16EYI10WB6g74sGmRwSNXjPQnYUKYotKkvLGpshucCaeWZMOylnc6o2AgDX9grhQQx7fm2DJRTyuNhUlwmEoWhjoG3F0ySAWUsEbH3bJMsEBwoB//0qmFJob74ffdaslqL+IrSy7LJ76/G5TkvNC+J0VQkpH1u3iJJs0uUYyLDzdBIQ6Ogd8LDQ3VKnJLm4g/uDLe+G7zzzkOPzCjXL+70naaQ9medzqmh+/SmaQ6uFWLDQLcRln++wBwoEibNpG4uOJvqXy+ql50DjlNchSuqLmeadFoo9/mDT0q3G7o/80P15ostktjb7h9bfNc+nZVSnUEJXbCjTeqS5UYuxn+HTS5nZsPVxJA2O5GdKCYK4x8lTTKShRstqPfbQpplfllx2fwXcSljuYi3YipPyS3GCAqf5A7aYYwJ7AvGqUiR2SsVQ9Nbp8MGHET1GxhifC692APj6SJxZD3i1drSYZPMMsS9rKAJTGz2FEupohtpf2tgXm6c16nDk/cw+C7K7me5j5PLHv55DFCS84b06AytZPdkFZLj7FHOkcFGJXitHkX5cgww7vuf6F3p0yM/W73SoXTx6GX4G6Hg2rYx3O/9VU2Uq8lvURB4qIbD9XQpzmyiFMaytMnqxcZJcoXCtfkTJ6pI7a92JpRUvdSitg967VUDUAQnCXCM/m0snRkR9LtoXAO1FUGpwlp1EfIdCZFPKNnXMeqev0j9W9ZrkEs9ZWcUEexSj5z+dKYQBhIICviYUQHVqBTZSNy22PlUIeDeIs11j7q4t8rD8LPvzAKWVqXE+5lS1JPZkjg4y5hfX1Dod3t96clFfwsvDP6xBSe1NBcoKbkyGxYK0UvPGtKQEE0Se2zAymYDv41klYE9s+rxp8e94/H8XhrL9oGm8KWb2RmYnAE7ry9gd6e8ZuBRIsISlJAE/e8y8xFmP031S6Lnaet6YXPsFpuFsdQs535IjcFd75hh6DNMBYhSfjv456cvhsb99+fRw/KVZLC3yzNSCbLSyo9d9BI45Plma6V8akURQA/qsaAzU0VyTIqZJkPDTzhuCl92vD2AD/QOhx6iwRSVPAxcRFZcWjgc2wCKh+uCYkTVbNQpB9B90YlNmI3fWTuUOUjwOzQRxJZj11NsimjOJ50qQwTTFj6qQvQ1a/I+MkTx5UO+yNHl718JWcR3AXGmv/aa9rD1eNP8ioTGlOZwPgmr2sor2iBpKTOrB83QgZXP+xRYkb4zVC+LoAXEoIa1+zArywlgREer7DLePukkU6wHTkuSaF+ge5Of1bXuU4i938WJHj0t3D8uQxkJvoFi/EYN/7u2P1zGRLV4dHVUsZMGCCtnO6BBigFMAA=");
        httpPost.setHeader("referer", "https://wx.qq.com/?&lang=zh_CN&target=t");

        httpPost.setEntity(reqEntity);
        try {
            CloseableHttpResponse response = myHttpClient.execute(httpPost);
            entity = response.getEntity();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return entity;
    }

}