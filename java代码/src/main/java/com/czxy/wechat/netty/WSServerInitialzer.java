package com.czxy.wechat.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 20:44 2019-09-08
 */
public class WSServerInitialzer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        //webSocket基于http协议，所以要有http编解码器
        pipeline.addLast(new HttpServerCodec());
        //对写大数据流的支持
        pipeline.addLast(new ChunkedWriteHandler());
        //对HttpMessage 进行聚合，聚合成HttpRequest或者HttpResponse
        //几乎在netty编程中都会用到此注解
        pipeline.addLast(new HttpObjectAggregator(1024*6));

        //==========================以上是用于对Http协议的支持===========================================



        // ====================== 增加心跳支持 start    ======================
        // 针对客户端，如果在1分钟时没有向服务端发送读写心跳(ALL)，则主动断开
        // 如果是读空闲或者写空闲，不处理
        pipeline.addLast(new IdleStateHandler(8, 10, 60));
        // 自定义的空闲状态检测
        pipeline.addLast(new HeartBeatHandler());
        // ====================== 增加心跳支持 end    ======================

        // ====================== 以下是支持httpWebsocket ======================
        /**
         *webSocket服务器处理的协议。用于指定给客户端访问的路由 ：/ws
         * 本Handler会帮你处理繁重的复杂的事情
         * 会帮你处理 我手动在 handshaking(close,ping,pong) ping + pong = 心跳
         * 对于websocket 来讲都是以frames进行传输的，不同类型的数据对应的frames也不同
         */
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        //添加自定义的Handler
        pipeline.addLast(new ChatHandler());



    }
}
