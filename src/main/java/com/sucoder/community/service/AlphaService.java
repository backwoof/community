package com.sucoder.community.service;

import com.sucoder.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class AlphaService {
    public AlphaService(){
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init(){
        System.out.println("初始化AlphaService方法");
    }

    @PreDestroy
    public void destroy(){
        System.out.println("销毁AlphaService方法");
    }

    @Autowired
    private AlphaDao alphaDao;

    public void find(){
        System.out.println(alphaDao.select());
    }
}
