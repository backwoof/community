package com.sucoder.community.controller;

import com.sucoder.community.entity.Event;
import com.sucoder.community.entity.User;
import com.sucoder.community.event.EventProducer;
import com.sucoder.community.service.LikeService;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.CommunityUtil;
import com.sucoder.community.util.HostHolder;
import com.sucoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {
    @Autowired
    private LikeService likeService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/like",method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType,int entityId,int entityUserId,int postId){
        User user = hostHolder.getUser();
        //点赞
        likeService.like(user.getId(), entityType,entityId,entityUserId);
        long entityLikeCount = likeService.findEntityLikeCount(entityType, entityId);
        int entityLikeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        Map<String,Object> map = new HashMap<>();
        //返回的结果
        map.put("entityLikeCount",entityLikeCount);
        map.put("entityLikeStatus",entityLikeStatus);

        //触发点赞事件
        if (entityLikeStatus==1){//是点赞,而不是取消赞
            Event event = new Event().setTopic(TOPIC_LIKE).setUserId(user.getId()).setEntityType(entityType)
                    .setEntityId(entityId).setEntityUserId(entityUserId)
                    .setData("postId",postId);
            eventProducer.fireEvent(event);
        }

        //计算帖子分数
        if (entityType==ENTITY_TYPE_POST){
            String postScoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(postScoreKey,postId);
        }
        return CommunityUtil.getJSONString(0,null,map);

    }
}
