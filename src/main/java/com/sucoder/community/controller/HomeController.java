package com.sucoder.community.controller;

import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.entity.Page;
import com.sucoder.community.service.DiscussPostService;
import com.sucoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/community")
public class HomeController {
    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @RequestMapping(path = "/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page){
        //方法调用前,SpringMVC会自动实例化Model和Page,并将Page注入Model
        //所以,在Thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.findDiscussPostRow(0));
        page.setPath("/community/index");

        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        List<Map<String,Object>> list = new ArrayList<>();
        if (discussPosts!=null){
            for (DiscussPost discussPost : discussPosts) {
                Map<String,Object> map = new HashMap<>();
                map.put("post",discussPost);
                map.put("user",userService.findUserById(discussPost.getUserId()));
                list.add(map);
            }
        }
        model.addAttribute("list",list);
        return "/index";
    }

}
