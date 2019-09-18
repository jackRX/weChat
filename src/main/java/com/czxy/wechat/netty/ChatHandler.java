package com.czxy.wechat.netty;

import com.czxy.SpringUtil;
import com.czxy.wechat.enums.MsgActionEnum;
import com.czxy.wechat.service.Impl.UserServiceImpl;
import com.czxy.wechat.service.UserService;
import com.czxy.wechat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: huangfurong
 * @Description:处理消息的handler TextWebSocketFrame: 在netty中，适用于专门为webSocket专门处理文本的对象，frame是消息的载体
 * @Date: Create in 21:28 2019-09-08
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    //用于记录和管理所有客户端的Chaneel
    protected static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        Channel currentChannel = ctx.channel();
        // 1.获取客户端发过来的消息
        String content = msg.text();
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer actiontype = dataContent.getAction();
        // 2.判断消息类型，根据不同的类型来处理不同的业务
        if (actiontype.equals(MsgActionEnum.CONNECT.type)) {
            //2.1 当weebSocket 第一次open的时候，初始化channel,把Channel和用户的userId关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannelRel.put(senderId, currentChannel);

//打印测试
//            for (Channel channel : users) {
//                System.out.println(channel.id().asLongText());
//            }
//            UserChannelRel.output();

        } else if (actiontype.equals(MsgActionEnum.CHAT.type)) {
            //2.2 聊天类型的消息，把聊天记录保存到数据库，并标记消息的签收状态【未签收】
            ChatMsg chatMsg = dataContent.getChatMsg();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();
            String chatMsgMsg = chatMsg.getMsg();

            //保存到数据库，并标记为未签收
            UserService userService = (UserService)SpringUtil.getBean("userServiceImpl");
            String msgId = userService.saveChatmsg(chatMsg);
            chatMsg.setMsgId(msgId);

            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);

            //发送消息
            //重全局的Channel中获取接收方的Channel
            Channel receiverChannel = UserChannelRel.get(receiverId);
            if (receiverChannel == null) {
                // TODO Channel为空代表用户不在线，推送消息 （JPush,个推，小米推送）

            } else {
                //当receiverChannel，不为空时，说明用户在线，从ChannelGroup中查询对应的Channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null) {
                    //用户在线
                    receiverChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContentMsg)));
                } else {
                    //用户离线，推送
                }
            }

        } else if (actiontype.equals(MsgActionEnum.SIGNED.type)) {
            //2.3 签收消息类型，针对具体的消息进行签收，修改数据库中的对应消息的签收状态【已签收】
            UserService userService = (UserService) SpringUtil.getBean("userServiceImpl");
            //扩展字段在signed类型的消息中，代表需要去签收的消息id,逗号间隔
            String msgIdsStr = dataContent.getExtand();
            String[] msgIds = msgIdsStr.split(",");
            //把id放进 数组
            List<String> msgIdList = new ArrayList<>();
            for (int i = 0; i < msgIds.length; i++) {
                if (StringUtils.isNotBlank(msgIds[i])) {
                    msgIdList.add(msgIds[i]);
                }
            }
            System.out.println(msgIdList.toString());

            if (msgIdList != null && !msgIdList.isEmpty() && msgIdList.size() > 0) {
                //批量签收
                userService.updateMsgSigned(msgIdList);
            }
        } else if (actiontype.equals(MsgActionEnum.KEEPALIVE.type)) {
            //2.4 心跳类型的消息
            System.out.println("收到来自channel为：【"+currentChannel +"】的心跳包。。。");
        }
    }

    /**
     * 当客户端连接服务器端之后（打开连接）
     * 获取客户端的Channel并放到channelGroup去管理
     *
     * @param ctx
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //当触发handlerRemoved会自动移除ChannelGroup里对应客户端的Channel
        users.remove(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        //发生异常后关闭连接（关闭channel），随后从ChannelGroup中移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }

}
