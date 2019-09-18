package com.czxy.wechat.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 20:34 2019-09-08
 */
@Component
public class WSServer {

    public static class SingletionWSServer{
        static  final WSServer instance = new WSServer();
    }

    public static WSServer getInstance(){
        return SingletionWSServer.instance;
    }

    //主线程
    private EventLoopGroup mainGroup;
    //父线程
    private  EventLoopGroup  subGroup ;
    //启动器
    private ServerBootstrap serverBootstrap;

    private ChannelFuture future;

    public WSServer(){
         mainGroup = new NioEventLoopGroup();
          subGroup = new NioEventLoopGroup();
        serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(mainGroup,subGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInitialzer());

    }

    public void start(){
        this.future = serverBootstrap.bind(8088);
        System.err.println("netty websocket server 启动完毕。。。");
    }


//    public static void main(String[] args) throws Exception {
//
//        EventLoopGroup mainGroup = new NioEventLoopGroup();
//        EventLoopGroup  subGroup = new NioEventLoopGroup();
//
//        try {
//            ServerBootstrap serverBootstrap = new ServerBootstrap();
//            serverBootstrap.group(mainGroup,subGroup)
//                    .channel(NioServerSocketChannel.class)
//                    .childHandler(null);
//
//            ChannelFuture future= serverBootstrap.bind(8088).sync();
//
//            future.channel().closeFuture().sync();
//        }finally {
//            mainGroup.shutdownGracefully();
//            subGroup.shutdownGracefully();
//        }
//
//
//    }

}
