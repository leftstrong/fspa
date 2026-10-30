package com.zbkj.service.util;

import cn.hutool.core.io.FileUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * 调用微信支付被扫支付 工具类
 */
public class WeChatUtil {
    // 微信支付被扫支付接口地址
    public static final String PAY_MICROPAY_URL = "https://api.mch.weixin.qq.com/pay/micropay";
    // 微信支付被扫支付查询接口地址
    public static final String PAY_MICROPAY_QUERY_URL = "https://api.mch.weixin.qq.com/pay/orderquery";
    // 微信支付被扫支付订单撤销接口地址
    public static final String PAY_MICROPAY_REVERSE_API_URL = "https://api.mch.weixin.qq.com/secapi/pay/reverse";
    // 微信支付被扫支付订单退款接口地址
    public static final String PAY_MICROPAY_REFUND_API_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";

    // 微信支付商户号
    public static final String MCH_ID = "1636382643";
    // 微信支付密钥
    public static final String KEY = "5pNEorRPUOjtE6xpBb0Nnv1GhMaioyw2";
    //公众号微信号（支付）
    public static final String APPID = "wxaab92e75d07c9766";
    public static final String CERT_PATH = "/usr/local/cert/apiclient_cert.p12"; // 证书路径
    public static final String CERT_PASSWORD = "1636382643"; // 证书密码
    private static final int CONNECT_TIMEOUT = 5000; // 连接超时时间
    private static final int SOCKET_TIMEOUT = 5000; // 响应超时时间

    /**
     * 生成随机字符串
     *
     * @return 随机字符串
     */
    public static String generateNonceStr() {
        return DigestUtils.md5Hex(String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 生成签名
     *
     * @param paramMap 参数列表
     * @param key      商户支付密钥
     * @return 签名
     */
    public static String generateSignature(SortedMap<String, Object> paramMap, String key) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (v != null && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k).append("=").append(v).append("&");
            }
        }
        sb.append("key=").append(key);
        return DigestUtils.md5Hex(sb.toString()).toUpperCase();
    }

    /**
     * 将Map转换为XML格式
     *
     * @param map 参数列表
     * @return XML格式的字符串
     */
    public static String mapToXml(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("<").append(entry.getKey()).append(">");
            sb.append("<![CDATA[").append(entry.getValue()).append("]]>");
            sb.append("</").append(entry.getKey()).append(">");
        }
        sb.append("</xml>");
        return sb.toString();
    }

    /**
     * 将XML格式的字符串转换为Map
     *
     * @param xml XML格式的字符串
     * @return Map对象
     */
    public static Map<String, String> xmlToMap(String xml) {
        Map<String, String> map = new HashMap<>();
        try {
            Document document = DocumentHelper.parseText(xml);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element : elements) {
                map.put(element.getName(), element.getText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    // 发送post请求，使用证书进行签名验证
    public static String post(String url, String data, String certPath, String certPassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File file = FileUtil.file(certPath);
        FileInputStream stream = new FileInputStream(file);
        try {
            keyStore.load(stream, certPassword.toCharArray());
        } finally {
            stream.close();
        }
        SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keyStore, certPassword.toCharArray()).build();
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPost.setConfig(requestConfig);
        httpPost.setEntity(new ByteArrayEntity(data.getBytes("UTF-8")));
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        String result = EntityUtils.toString(httpEntity, "UTF-8");
        EntityUtils.consume(httpEntity);
        return result;
    }
}
