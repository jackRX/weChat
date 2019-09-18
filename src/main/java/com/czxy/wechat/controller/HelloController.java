package com.czxy.wechat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 15:20 2019-09-09
 */
@RestController
public class HelloController {


    @GetMapping("/hello")
    public String hello(){
        return "hello wechat";
    }

}
