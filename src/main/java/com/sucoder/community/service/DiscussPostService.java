package com.sucoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.sucoder.community.dao.DiscussPostMapper;
import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.util.SensitiveFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //caffeine核心接口：cache，LoadingCache，AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public List<DiscussPost> load(String key) throws Exception {
                        if (key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //可以加二级缓存: Redis -> mysql
                        logger.debug("load post list from DB");
                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });
        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(Integer key) throws Exception {
                        logger.debug("load post rows from DB");
                        return discussPostMapper.selectDiscussPostRow(key);
                    }
                });

    }
    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit,int orderMode){
        if (userId==0 && orderMode==1){
            return postListCache.get(offset+":"+limit);
        }
        logger.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    public int findDiscussPostRow(int userId){
        if (userId == 0){
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRow(userId);
    }

    public int addDiscussPost(DiscussPost discussPost){
        if (discussPost==null){
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        //过滤敏感词
        discussPost.setTitle(sensitiveFilter.filterWords(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filterWords(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPost(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }
    public int updateScore(int id,double score){
        return discussPostMapper.updateScore(id,score);
    }
}
