package com.zbkj.front.config;

import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.interceptor.SwaggerInterceptor;
import com.zbkj.front.filter.ResponseFilter;
import com.zbkj.front.interceptor.FrontTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * token验证拦截器
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2022 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 这里使用一个Bean为的是可以在拦截器中自由注入，也可以在拦截器中使用SpringUtil.getBean 获取
    // 但是觉得这样更优雅

    @Bean
    public HandlerInterceptor frontTokenInterceptor(){
        return new FrontTokenInterceptor();
    }

    @Bean
    public ResponseFilter responseFilter(){ return new ResponseFilter(); }

    @Value("${swagger.basic.username}")
    private String username;
    @Value("${swagger.basic.password}")
    private String password;
    @Value("${swagger.basic.check}")
    private Boolean check;

    @Autowired
    private CrmebConfig crmebConfig;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //添加token拦截器
        //addPathPatterns添加需要拦截的命名空间；
        //excludePathPatterns添加排除拦截命名空间

        //前端用户登录token
        registry.addInterceptor(frontTokenInterceptor()).
                addPathPatterns("/api/front/**").
                excludePathPatterns("/api/front/index").
                excludePathPatterns("/api/front/qrcode/**").
                excludePathPatterns("/api/front/login/mobile").
                excludePathPatterns("/api/front/login").
                excludePathPatterns("/api/front/sendCode").
                excludePathPatterns("/api/front/wechat/**").
                excludePathPatterns("/api/front/search/keyword").
                excludePathPatterns("/api/front/share").
                excludePathPatterns("/api/front/article/**").
                excludePathPatterns("/api/front/city/**").
                excludePathPatterns("/api/front/product/hot").
                excludePathPatterns("/api/front/product/good").
                excludePathPatterns("/api/front/products/**").
                excludePathPatterns("/api/front/reply/**").
                excludePathPatterns("/api/front/user/service/**").
                excludePathPatterns("/api/front/logistics").
                excludePathPatterns("/api/front/groom/list/**").
                excludePathPatterns("/api/front/config").
                excludePathPatterns("/api/front/category").
                excludePathPatterns("/api/front/seckill/*").
                excludePathPatterns("/api/front/seckill/list/*").
                excludePathPatterns("/api/front/seckill/detail/*").
                excludePathPatterns("/api/front/ios/*").
                excludePathPatterns("/api/front/ios/register/binding/phone").
                excludePathPatterns("api/front/combination/index").
                excludePathPatterns("api/front/seckill/index").
                excludePathPatterns("api/front/bargain/index").
                excludePathPatterns("api/front/combination/index").
                excludePathPatterns("api/front/index/product/*").
                excludePathPatterns("api/front/index/color/config").
                excludePathPatterns("api/front/image/domain").
                excludePathPatterns("api/front/product/leaderboard").
                // 良物换物：游客可浏览好物信息流/详情/等级，提升转化（先逛后登录）
                // 注意：发布/置顶/我的发布/申请/换物单 等仍须登录，勿放开
                excludePathPatterns("/api/front/barter/item/list").
                excludePathPatterns("/api/front/barter/item/info").
                excludePathPatterns("/api/front/barter/item/cities").
                excludePathPatterns("/api/front/barter/category/list").
                excludePathPatterns("/api/front/barter/level/list").
                excludePathPatterns("/swagger-resources/**", "/webjars/**", "/v2/**", "/swagger-ui.html/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前台上传的图片（imagePath 目录下）对外可访问，否则上传成功也无法展示(404)
        String imagePath = crmebConfig.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            String loc = imagePath.endsWith("/") ? imagePath : imagePath + "/";
            // 上传文件实际落盘在 imagePath/crmebimage/ 下，这里把 /crmebimage/** 也映射出去，
            // 否则即便 URL 拼对了也会 404。注意与 admin 端一致。
            registry.addResourceHandler("/crmebimage/**")
                    .addResourceLocations("file:" + loc + "crmebimage/");
            registry.addResourceHandler("/image/**")
                    .addResourceLocations("file:" + loc);
        }
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public FilterRegistrationBean filterRegister()
    {
        //注册过滤器
        FilterRegistrationBean registration = new FilterRegistrationBean(responseFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    /* 必须在此处配置拦截器,要不然拦不到swagger的静态资源 */
    @Bean
    @ConditionalOnProperty(name = "swagger.basic.enable", havingValue = "true")
    public MappedInterceptor getMappedInterceptor() {
        return new MappedInterceptor(new String[]{"/doc.html", "/webjars/**"}, new SwaggerInterceptor(username, password, check));
    }
}
