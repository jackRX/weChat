package com.czxy.wechat.service.Impl;

import com.czxy.wechat.enums.MsgActionEnum;
import com.czxy.wechat.enums.MsgSignFlagEnum;
import com.czxy.wechat.enums.SearchFriendsStatusEnum;
import com.czxy.wechat.mapper.*;
import com.czxy.wechat.netty.ChatMsg;
import com.czxy.wechat.netty.DataContent;
import com.czxy.wechat.netty.UserChannelRel;
import com.czxy.wechat.pojo.FriendsRequest;
import com.czxy.wechat.pojo.MyFriends;
import com.czxy.wechat.pojo.Users;
import com.czxy.wechat.pojo.vo.FriendRequestVO;
import com.czxy.wechat.pojo.vo.MyFriendsVO;
import com.czxy.wechat.service.UserService;
import com.czxy.wechat.utils.FastDFSClient;
import com.czxy.wechat.utils.FileUtils;
import com.czxy.wechat.utils.JsonUtils;
import com.czxy.wechat.utils.QRCodeUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 17:20 2019-09-09
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private MyFriendsMapper myFriendsMapper;

    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Autowired
    private UsersMapperCustom usersMapperCustom;

    @Autowired
    private Sid sid;

    @Autowired
    private QRCodeUtils qrCodeUtils;

    @Autowired
    private FastDFSClient fastDFSClient;


    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean quryUserNameIsExist(String userName) {

        Users users = new Users();
        users.setUsername(userName);

        Users selectOne = usersMapper.selectOne(users);

        return selectOne != null ? true : false;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String userName, String password) {

        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();

        criteria.andEqualTo("username", userName);
        criteria.andEqualTo("password", password);

        Users result = usersMapper.selectOneByExample(example);

        return result;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users saveUsers(Users users) {

        String userId = sid.nextShort();

        //为每个用户生成一个唯一的二维码
        String qrCodePath = "E://user" + userId + "qrcode.png";
        //wechat_qrcode:[username]
        qrCodeUtils.createQRCode(qrCodePath, "wechat_qrcode:" + users.getUsername());
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);

        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        users.setQrcode(qrCodeUrl);

        users.setId(userId);
        usersMapper.insert(users);

        return users;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users updateUserInfo(Users users) {
        usersMapper.updateByPrimaryKeySelective(users);
        Users userByid = queryUserByid(users.getId());
        return userByid;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriends(String myUserId, String friendUserName) {

        //1.搜索的用户如果不存在，返回【无此用户】
        Users byUserName = queryUserByUserName(friendUserName);
        if (byUserName == null) {
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        // 2.搜索的账号是你自己，返回【不能添加自己】
        if (myUserId.equals(byUserName.getId())) {
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }

        //3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Example example = new Example(MyFriends.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("myUserId", myUserId);
        criteria.andEqualTo("myFriendUserId", byUserName.getId());
        MyFriends myFriends = myFriendsMapper.selectOneByExample(example);
        if (myFriends != null) {
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserByUserName(String userName) {
        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username", userName);
        return usersMapper.selectOneByExample(userExample);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendsRequest(String myUserId, String friendUserName) {
        //根据用户名把用户信息找出来
        Users friend = queryUserByUserName(friendUserName);

        //查询发送好友请求记录
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria criteria = fre.createCriteria();
        criteria.andEqualTo("sendUserId", myUserId);
        criteria.andEqualTo("acceptUserId", friend.getId());

        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(fre);

        if (friendsRequest == null) {
            //如果不是你的好友，并且好友记录没有添加，则新增好有记录

            String requestId = sid.nextShort();

            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());

            friendsRequestMapper.insert(request);
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example fre = new Example(FriendsRequest.class);
        Example.Criteria criteria = fre.createCriteria();
        criteria.andEqualTo("sendUserId", sendUserId);
        criteria.andEqualTo("acceptUserId", acceptUserId);
        friendsRequestMapper.deleteByExample(fre);

    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId, acceptUserId);
        saveFriends(acceptUserId, sendUserId);
        deleteFriendRequest(acceptUserId, sendUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if (sendChannel != null) {
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        List<MyFriendsVO> myFriendsVOS = usersMapperCustom.queryMyFriends(userId);
        return myFriendsVOS;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveChatmsg(ChatMsg chatMsg) {
        com.czxy.wechat.pojo.ChatMsg msgDB = new com.czxy.wechat.pojo.ChatMsg();

        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());

        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgList) {
        usersMapperCustom.batchUpdateMsgSigned(msgList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public  List<com.czxy.wechat.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        Example chatExample = new Example(com.czxy.wechat.pojo.ChatMsg.class);
        Example.Criteria criteria = chatExample.createCriteria();
        criteria.andEqualTo("acceptUserId", acceptUserId);
        criteria.andEqualTo("signFlag", 0);
        List<com.czxy.wechat.pojo.ChatMsg> chatMsgList = chatMsgMapper.selectByExample(chatExample);
        return chatMsgList;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        String recordId = sid.nextShort();
        myFriends.setId(recordId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);
    }


    private Users queryUserByid(String userId) {
        Users users = usersMapper.selectByPrimaryKey(userId);
        return users;
    }
}
