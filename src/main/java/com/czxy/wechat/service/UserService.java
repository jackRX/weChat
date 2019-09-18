package com.czxy.wechat.service;

import com.czxy.wechat.netty.ChatHandler;
import com.czxy.wechat.netty.ChatMsg;
import com.czxy.wechat.pojo.Users;
import com.czxy.wechat.pojo.vo.FriendRequestVO;
import com.czxy.wechat.pojo.vo.MyFriendsVO;

import java.util.List;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 17:15 2019-09-09
 */
public interface UserService {

    /**
     * 判断用户名是否存在
     *
     * @param userName
     * @return
     */
    public boolean quryUserNameIsExist(String userName);

    /**
     * 查询用户是否存在
     *
     * @param userName
     * @param password
     * @return
     */
    public Users queryUserForLogin(String userName, String password);


    /**
     * 用户注册
     *
     * @param users
     * @return
     */
    public Users saveUsers(Users users);

    /**
     * 修改用户密码
     *
     * @param users
     */
    public Users updateUserInfo(Users users);

    /**
     * 搜索朋友的前置条件
     */
    public Integer preconditionSearchFriends(String myUserName, String friendUserName);

    /**
     * 根据用户名查询用户对象
     *
     * @param userName
     * @return
     */
    public Users queryUserByUserName(String userName);


    /**
     * 添加好友请求记录保存到数据库
     */
    public void sendFriendsRequest(String myUserId, String friendUserName);

    /**
     * 查询自己的好友请求列表
     *
     * @param acceptUserId
     * @return
     */
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);


    /**
     * 删除好友请求记录
     *
     * @param sendUserId
     * @param acceptUserId
     */
    public void deleteFriendRequest(String sendUserId, String acceptUserId);

    /**
     * 同过好友请求
     *
     * @param sendUserId
     * @param acceptUserId
     */
    public void passFriendRequest(String sendUserId, String acceptUserId);

    /**
     * 查询我的好友列表
     *
     * @param userId
     * @return
     */
    public List<MyFriendsVO> queryMyFriends(String userId);

    /**
     * 保存聊天记录到数据库
     *
     * @param chatMsg
     * @return
     */
    public String saveChatmsg(ChatMsg chatMsg);

    /**
     * 批量签收消息
     *
     * @param msgList
     */
    public void updateMsgSigned(List<String> msgList);

    /**
     * 获取未签收的消息列表
     * @param acceptUserId
     * @return
     */
    public List<com.czxy.wechat.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);


}
