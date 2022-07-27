package com.sucoder.community;

import com.sucoder.community.dao.DiscussPostMapper;
import com.sucoder.community.dao.LoginTicketMapper;
import com.sucoder.community.dao.MessageMapper;
import com.sucoder.community.dao.UserMapper;
import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.entity.LoginTicket;
import com.sucoder.community.entity.Message;
import com.sucoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private MessageMapper messageMapper;

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
        user.setCreateTime(new Date());
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
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(101, 0, 10,0);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
        int i = discussPostMapper.selectDiscussPostRow(0);
        System.out.println(i);
    }

    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
        loginTicketMapper.updateStatus("abc", 1);
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectLetters(){
        List<Message> messages = messageMapper.selectConversations(111, 0, 10);
        for (Message message : messages) {
            System.out.println(message);
        }
    }
}
