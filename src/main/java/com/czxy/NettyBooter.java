package com.czxy;

import com.czxy.wechat.netty.WSServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 16:18 2019-09-09
 */
@Component
public class NettyBooter implements ApplicationListener<ContextRefreshedEvent> {


    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if(event.getApplicationContext().getParent() == null){
            try{
                WSServer.getInstance().start();
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }
}
