package com.czxy.wechat.controller;

import com.czxy.wechat.enums.OperatorFriendRequestTypeEnum;
import com.czxy.wechat.enums.SearchFriendsStatusEnum;
import com.czxy.wechat.mapper.UsersMapperCustom;
import com.czxy.wechat.pojo.ChatMsg;
import com.czxy.wechat.pojo.Users;
import com.czxy.wechat.pojo.bo.UsersBO;
import com.czxy.wechat.pojo.vo.MyFriendsVO;
import com.czxy.wechat.pojo.vo.UsersVO;
import com.czxy.wechat.service.UserService;
import com.czxy.wechat.utils.FastDFSClient;
import com.czxy.wechat.utils.FileUtils;
import com.czxy.wechat.utils.IMoocJSONResult;
import com.czxy.wechat.utils.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.util.List;

/**
 * @Author: huangfurong
 * @Description:
 * @Date: Create in 16:47 2019-09-09
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    public IMoocJSONResult registOrLogin(@RequestBody Users user) throws Exception {
        //1，判断用户名和密码不能为空
        if (StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())) {
            return IMoocJSONResult.errorMsg("用户名和密码不能为空");
        }

        //2.判断用户名是否存在，存在则登入不存在则注册
        boolean userNameIsExist = userService.quryUserNameIsExist(user.getUsername());

        Users userResult = null;
        if (userNameIsExist) {
            //2.1登入
            userResult = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if (userResult == null) {
                return IMoocJSONResult.errorMsg("用户名或密码不正确。。");
            }
        } else {
            //2.2注册
            user.setNickname(user.getUsername());
            user.setFaceImage("");
            user.setFaceImageBig("");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));

            userResult = userService.saveUsers(user);

        }

        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userResult, usersVO);

        return IMoocJSONResult.ok(usersVO);
    }

    @PostMapping("/uploadFaceBase64")
    public IMoocJSONResult uploadFace(@RequestBody UsersBO usersBO) throws Exception {

        //1.获取前端传过来的Base64字符串，然后转换成文件后再上传
        String base64Data = usersBO.getFaceData();
        String userFacePath = "E:\\" + usersBO.getUserId() + "userface64.png";
        FileUtils.base64ToFile(userFacePath, base64Data);

        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        //获取缩略图地址
        String thump = "_80x80.";
        String[] arr = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        //更新用户信息
        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setFaceImageBig(url);
        user.setFaceImage(thumpImgUrl);

        Users userInfo = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(userInfo);
    }

    @PostMapping("/setNickName")
    public IMoocJSONResult setNickName(@RequestBody UsersBO usersBO) throws Exception {

        //更新用户信息
        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setNickname(usersBO.getNickname());

        Users userInfo = userService.updateUserInfo(user);

        return IMoocJSONResult.ok(userInfo);
    }

    /**
     * 搜索接口，根据账号做匹配查询而不是模糊查询
     *
     * @param myUserId
     * @param friendUsername
     * @return
     * @throws Exception
     */
    @PostMapping("/search")
    public IMoocJSONResult search(String myUserId, String friendUsername) throws Exception {

        //1.判断myUserName和friendUserName不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }
        //前置条件 - 1.搜索的用户如果不存在，返回【无此用户】
        //前置条件 - 2.搜索的账号是你自己，返回【不能添加自己】
        //前置条件 - 3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        int status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            Users users = userService.queryUserByUserName(friendUsername);
            UsersVO usersVO = new UsersVO();
            BeanUtils.copyProperties(users, usersVO);
            return IMoocJSONResult.ok(usersVO);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }

    }


    /**
     * 好忧伤搜索接口，根据账号做匹配查询而不是模糊查询
     *
     * @param myUserId
     * @param friendUsername
     * @return
     * @throws Exception
     */
    @PostMapping("/addFriendRequest")
    public IMoocJSONResult addFriendRequest(String myUserId, String friendUsername) throws Exception {

        //1.判断myUserName和friendUserName不能为空
        if (StringUtils.isBlank(myUserId) || StringUtils.isBlank(friendUsername)) {
            return IMoocJSONResult.errorMsg("");
        }
        //前置条件 - 1.搜索的用户如果不存在，返回【无此用户】
        //前置条件 - 2.搜索的账号是你自己，返回【不能添加自己】
        //前置条件 - 3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        int status = userService.preconditionSearchFriends(myUserId, friendUsername);
        if (status == SearchFriendsStatusEnum.SUCCESS.status) {
            userService.sendFriendsRequest(myUserId, friendUsername);
        } else {
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return IMoocJSONResult.errorMsg(errorMsg);
        }
        return IMoocJSONResult.ok();
    }

    /**
     * 查询自己的好友请求列表
     */
    @PostMapping("/queryFriendRequests")
    public IMoocJSONResult queryFriendRequestList(String userId) throws Exception {

        //1.判断id不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }

        //查询用户接收到的好友申请
        return IMoocJSONResult.ok(userService.queryFriendRequestList(userId));
    }


    /**
     * 好友请求接收方通过或者忽略好友请求
     */
    @PostMapping("/operFriendRequest")
    public IMoocJSONResult operFriendRequest(String acceptUserId, String sendUserId, Integer operType) throws Exception {

        //1.判断acceptUserId,sendUserId,operType不能为空
        if (StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType == null) {
            return IMoocJSONResult.errorMsg("");
        }

        //  如果 operType 没有对应的枚举值 则抛出对应的null信息
        if (StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))) {
            return IMoocJSONResult.errorMsg("");
        }

        if (operType.equals(OperatorFriendRequestTypeEnum.IGNORE.type)) {
            //如果是忽略好友请求，则删除对应的好友请求记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);
        } else if (operType.equals(OperatorFriendRequestTypeEnum.PASS.type)) {
            //如果是通过好友请求，则相互添加数据库到数据库对应的表
            userService.passFriendRequest(sendUserId, acceptUserId);
            //然后删除好友请求的数据库记录
            userService.deleteFriendRequest(sendUserId, acceptUserId);

        }

        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);

        //查询用户接收到的好友申请
        return IMoocJSONResult.ok();
    }

    /**
     * 查询我的好友列表
     * @param userId
     * @return
     * @throws Exception
     */
    @PostMapping("/myFriends")
    public IMoocJSONResult myFriends(String userId) throws Exception {
        //1.判断userId不能为空
        if (StringUtils.isBlank(userId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //2.数据库查询好友列表
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);
        return IMoocJSONResult.ok(myFriends);
    }

    @PostMapping("/getUnReadMsgList")
    public IMoocJSONResult getUnReadMsgList(String acceptUserId) throws Exception {
        //1.判断 acceptUserId 不能为空
        if (StringUtils.isBlank(acceptUserId)) {
            return IMoocJSONResult.errorMsg("");
        }
        //2.数据库查询好友列表
        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);
        return IMoocJSONResult.ok(unReadMsgList);
    }
}
