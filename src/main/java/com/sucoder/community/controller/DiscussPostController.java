package com.sucoder.community.controller;

import com.sucoder.community.entity.*;
import com.sucoder.community.event.EventProducer;
import com.sucoder.community.service.CommentService;
import com.sucoder.community.service.DiscussPostService;
import com.sucoder.community.service.LikeService;
import com.sucoder.community.service.UserService;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.CommunityUtil;
import com.sucoder.community.util.HostHolder;
import com.sucoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user = hostHolder.getUser();
        if (user==null){
            return CommunityUtil.getJSONString(403,"您还没有登录");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);
        //触发发帖事件
        Event event = new Event().setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);
        //计算帖子分数
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(postScoreKey,post.getId());
        //报错的情况会统一处理
        return CommunityUtil.getJSONString(0,"发布成功");
    }

    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        DiscussPost discussPost = discussPostService.findDiscussPost(discussPostId);
        model.addAttribute("post",discussPost);
        //查作者
        User user = userService.findUserById(discussPost.getUserId());
        model.addAttribute("user",user);
        //点赞数
        long entityLikeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("entityLikeCount",entityLikeCount);
        //点赞状态
        int entityLikeStatus = hostHolder.getUser() == null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, discussPostId);
        model.addAttribute("entityLikeStatus",entityLikeStatus);
        //评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(discussPost.getCommentCount());
        //评论：给帖子的评论
        //回复：给评论的评论
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST
                ,discussPost.getId(),page.getOffset(), page.getLimit());
        //评论的VO列表
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if (commentList!=null){
            for (Comment comment : commentList) {
                //一个评论的VO
                Map<String,Object> map1 =  new HashMap<>();
                map1.put("comment",comment);
                map1.put("user",userService.findUserById(comment.getUserId()));
                map1.put("LikeCount1",likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT,comment.getId()));
                int likeStatus1 = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                map1.put("likeStatus1",likeStatus1);
                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT
                        , comment.getId(), 0, Integer.MAX_VALUE);
                //回复的VO列表
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                if (replyList!=null){
                    for (Comment reply : replyList) {
                        Map<String,Object> map2 = new HashMap<>();
                        map2.put("reply",reply);
                        map2.put("user",userService.findUserById(reply.getUserId()));
                        //回复的目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        map2.put("target",target);
                        map2.put("LikeCount2",likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT,reply.getId()));
                        int likeStatus2 = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        map2.put("likeStatus2",likeStatus2);
                        replyVoList.add(map2);
                    }
                }
                map1.put("replys",replyVoList);
                //回复数量
                int replyCount = commentService.findCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                map1.put("replyCount",replyCount);
                commentVoList.add(map1);
            }
        }
        model.addAttribute("comments",commentVoList);
        return "/site/discuss-detail";
    }

    //置顶
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);//1表示置顶
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }
    //加精
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);//1表示加精
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(postScoreKey,id);
        return CommunityUtil.getJSONString(0);
    }
    //删除
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);//1表示拉黑
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0);
    }

}
