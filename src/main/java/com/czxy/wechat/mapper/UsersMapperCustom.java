package com.czxy.wechat.mapper;

import com.czxy.wechat.pojo.Users;
import com.czxy.wechat.pojo.vo.FriendRequestVO;
import com.czxy.wechat.pojo.vo.MyFriendsVO;
import com.czxy.wechat.utils.MyMapper;

import java.util.List;



public interface UsersMapperCustom extends MyMapper<Users> {
	
	public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);
	
	public List<MyFriendsVO> queryMyFriends(String userId);
	
	public void batchUpdateMsgSigned(List<String> msgIdList);
	
}