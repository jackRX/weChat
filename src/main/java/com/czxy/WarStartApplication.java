package com.czxy;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @Author: huangfurong
 * @Description:使用外置tomcat启动application
 * @Date: Create in 3:42 2019-09-17
 */
public class WarStartApplication extends SpringBootServletInitializer {

    @Override
    public SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WechatApplication.class);
    }
}
