package com.sucoder.community.quartz;

import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.service.DiscussPostService;
import com.sucoder.community.service.ElasticsearchService;
import com.sucoder.community.service.LikeService;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job , CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    private static final Date epoch;//牛客纪元
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败"+e);
        }
    }
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postScoreKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(postScoreKey);
        if (operations.size()==0){
            logger.info("没有需要刷新的帖子");
            return;
        }
        logger.info("刷新帖子分数任务开始"+operations.size());
        while (operations.size()>0){
            this.refresh((Integer)operations.pop());
        }
        logger.info("刷新帖子分数任务结束");
    }
    private void refresh(int postId){
        DiscussPost discussPost = discussPostService.findDiscussPost(postId);
        if (discussPost==null){
            logger.error("帖子不存在,Id="+discussPost.getId());
            return;
        }
        //是否加精
        boolean isWonderful = discussPost.getStatus() == 1;
        //评论数量
        int commentCount = discussPost.getCommentCount();
        //点赞数量
        long entityLikeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        //计算权重
        double weight = (isWonderful?75:0)+commentCount*10+entityLikeCount*2;
        //分数=帖子权重+距离天数
        double score = Math.log10(Math.max(1,weight))+(discussPost.getCreateTime().getTime()-epoch.getTime())/(1000*60*60*24);
        //更新帖子分数
        discussPostService.updateScore(postId,score);
        //同步搜索数据
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
    }
}
