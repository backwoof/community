package com.sucoder.community;

import com.sucoder.community.dao.DiscussPostMapper;
import com.sucoder.community.dao.UserMapper;
import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(101);
        System.out.println(user);
        user = userMapper.selectByName("liubei");
        System.out.println(user);
        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("苏昌波");
        user.setPassword("19991210");
        user.setEmail("suchangbo2021@163.com");
        user.setSalt("1888");
        user.setType(0);
        user.setStatus(1);
        user.setActivationCode("854984");
        user.setCreatTime(new Date());
        user.setHeaderUrl("http://images.nowcoder.com/");

        int i = userMapper.insertUser(user);
        System.out.println(i);
        System.out.println(user.getId());
    }

    @Test
    public void testUpdateUser(){
        userMapper.updateStatus(150,0);
        userMapper.updatePassword(150,"153571984865");
        userMapper.updateHeader(150,"https://");
    }

    @Test
    public void testSelectDiscussPost(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(101, 0, 10);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
        int i = discussPostMapper.selectDiscussPostRow(0);
        System.out.println(i);
    }
}
