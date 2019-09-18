package com.czxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@ComponentScan(basePackages = {"com.czxy","org.n3r.idworker"})
@MapperScan(basePackages = "com.czxy.wechat.mapper")
@SpringBootApplication
public class WechatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatApplication.class, args);
    }

}
