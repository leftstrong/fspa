package com.zbkj.admin.filter;


import com.zbkj.common.utils.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 * 返回值输出过滤器
 */
//@Component
public class ResponseFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // 静态资源 / 上传图片是二进制流，一旦进 wrapper 就会被按 UTF-8 字符串重编码（非法字节变成 U+FFFD），
        // 结果是磁盘上的文件是好的、HTTP 读回来却是坏图。这类请求直接放行，不参与响应改写。
        if (isStaticResource((HttpServletRequest) request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletResponse resp = (HttpServletResponse) response;
        ResponseWrapper wrapperResponse = new ResponseWrapper(resp);//转换成代理类
        // 这里只拦截返回，直接让请求过去，如果在请求前有处理，可以在这里处理
        filterChain.doFilter(request, wrapperResponse);
        byte[] content = wrapperResponse.getContent();//获取返回值
        //判断是否有值
        if (content.length == 0) {
            return;
        }

        String contentType = wrapperResponse.getContentType();
        // 二进制响应（图片/音视频/字体/文件下载/代码生成器 zip）原样回写，绝不经过字符串编解码
        if (isBinaryContentType(contentType)) {
            resp.setContentLength(content.length);
            ServletOutputStream raw = resp.getOutputStream();
            raw.write(content);
            raw.flush();
            return;
        }

        String str = new String(content, StandardCharsets.UTF_8);
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            str = new ResponseRouter().filter(str, RequestUtil.getUri(req));
        } catch (Exception e) {
            e.printStackTrace();
        }
        //把返回值输出到客户端
        ServletOutputStream outputStream = resp.getOutputStream();
        if (str.length() > 0) {
            // 统一以 UTF-8 输出，避免 Windows 下 JVM 默认编码(GBK)导致中文乱码
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            if (contentType != null && !contentType.toLowerCase().contains("charset")) {
                resp.setContentType(contentType + ";charset=UTF-8");
            }
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentLength(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            //最后添加这一句，输出到客户端
            resp.flushBuffer();
        }
    }

    /**
     * 静态资源 / 上传图片 / 前端打包产物：直接放行
     */
    private boolean isStaticResource(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        return uri.startsWith("/crmebimage/")
                || uri.startsWith("/image/")
                || uri.startsWith("/static/")
                || uri.startsWith("/webjars/")
                || uri.endsWith("/doc.html")
                || uri.endsWith(".css")
                || uri.endsWith(".js")
                || uri.endsWith(".png")
                || uri.endsWith(".jpg")
                || uri.endsWith(".jpeg")
                || uri.endsWith(".gif")
                || uri.endsWith(".webp")
                || uri.endsWith(".svg")
                || uri.endsWith(".ico")
                || uri.endsWith(".woff")
                || uri.endsWith(".woff2")
                || uri.endsWith(".ttf");
    }

    /**
     * 明确的二进制类型（不含 text/json/xml 等需要改写字符集的内容）
     */
    private boolean isBinaryContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase();
        return ct.startsWith("image/")
                || ct.startsWith("audio/")
                || ct.startsWith("video/")
                || ct.startsWith("font/")
                || ct.startsWith("application/octet-stream")
                || ct.startsWith("application/pdf")
                || ct.startsWith("application/zip")
                || ct.startsWith("application/x-zip")
                || ct.contains("msword")
                || ct.contains("officedocument")
                || ct.contains("vnd.ms-");
    }
}
