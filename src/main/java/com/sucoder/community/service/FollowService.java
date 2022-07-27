package com.sucoder.community.service;

import com.sucoder.community.entity.User;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    public void follow(int userId,int entityType,int entityId){//关注
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);
                operations.multi();//开启事务
                operations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return operations.exec();
            }
        });
    }
    public void unfollow(int userId,int entityType,int entityId){//取关
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);
                operations.multi();//开启事务
                operations.opsForZSet().remove(followeeKey,entityId);
                operations.opsForZSet().remove(followerKey,userId);
                return operations.exec();
            }
        });
    }

    //查询关注的实体的数量
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询实体的粉丝数量
    public long findFollowerCount(int entityId,int entityType){
        String followerKey = RedisKeyUtil.getFollowerKey(entityId, entityType);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询当前用户关注实体的状态
    public boolean hasFollowed(int userId,int entityId,int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null;
    }

    //查询某用户关注的人
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset+limit-1);
        if (targetIds.size()==0){
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer target : targetIds) {
            Map<String,Object> map = new HashMap<>();
            User user = userService.findUserById(target);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, target);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
    //查询某用户的粉丝
    public List<Map<String,Object>> findFollowers(int entityId,int offset,int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(entityId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, limit);
        if (targetIds.size()==0){
            return null;
        }
        List<Map<String,Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            User user = userService.findUserById(targetId);
            Map<String,Object> map = new HashMap<>();
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
