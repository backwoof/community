package com.sucoder.community.service;

import com.sucoder.community.dao.AlphaDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class AlphaService {
    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);
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

    //可以让该方法在多线程环境下被异步调用
    @Async
    public void execute01(){
        logger.debug("execute01");
    }

//    @Scheduled(initialDelay = 10000,fixedRate = 1000)
//    public void execute02(){
//        logger.debug("execute02");
//    }
}
