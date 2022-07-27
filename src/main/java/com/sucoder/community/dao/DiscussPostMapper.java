package com.sucoder.community.dao;

import com.sucoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit,int orderMode);

    //@Param注解用于给参数取别名
    //如果只有一个参数,并且在<if>里使用，则必须加别名.
    int selectDiscussPostRow(@Param("userId") int userId);
    int insertDiscussPost(DiscussPost discussPost);
    DiscussPost selectDiscussPostById(int id);
    int updateCommentCount(int id,int commentCount);

    //修改帖子类型
    int updateType(int id,int type);
    //修改帖子状态
    int updateStatus(int id,int status);
    //修改帖子分数
    int  updateScore(int id,double score);
}
