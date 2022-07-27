package com.sucoder.community.controller;

import com.sucoder.community.service.DateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class DateController {
    @Autowired
    private DateService dateService;

    //统计页面
    @RequestMapping(path = "/data",method = {RequestMethod.GET,RequestMethod.POST})
    public String getDatePage(){
        return "/site/admin/data";
    }

    //统计网站UV
    @RequestMapping(path = "/data/uv",method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd")Date end,
                        Model model){
        long countUV = dateService.countUV(start, end);
        model.addAttribute("countUV",countUV);
        model.addAttribute("uvStartDate",start);
        model.addAttribute("uvEndDate",end);
        return "forward:/data";//转发，需要请求的方式相同，所以在统计页面上请求方法上多加了一个POST请求
//        return "/site/admin/data";
    }

    @RequestMapping(path = "/data/dau",method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd")Date end,
                         Model model){
        long countDAU = dateService.countDAU(start, end);
        model.addAttribute("countDAU",countDAU);
        model.addAttribute("dauStartDate",start);
        model.addAttribute("dauEndDate",end);
        return "forward:/data";
    }

}
