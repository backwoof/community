package com.sucoder.community;

import com.sucoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTests {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);
    //JDK线程池(都是通过工厂类实现的)
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    //JDK可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    //spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    //spring定时任务线程池
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private AlphaService alphaService;
    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //1、JDK普通线程池
    @Test
    public void testExecutorService(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello executor");
            }
        };
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }
        sleep(10000);
    }
    //JDK定时任务线程池
    @Test
    public void testScheduledExecutorService(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello ScheduledExecutorService");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(runnable,10000,1000, TimeUnit.MILLISECONDS);
        sleep(30000);
    }

    //Spring普通线程池
    @Test
    public void testThreadPoolTaskExecutor(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("Hello testThreadPoolTaskExecutor");
            }
        };
        for (int i = 0; i < 10; i++) {
            threadPoolTaskExecutor.submit(runnable);
        }
        sleep(10000);
    }

    //Spring定时任务线程池
    @Test
    public void testThreadPoolTaskScheduler(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.debug("test ThreadPoolTaskScheduler");
            }
        };
        Date start = new Date(System.currentTimeMillis()+10000);
        threadPoolTaskScheduler.scheduleAtFixedRate(runnable,start,1000);
        sleep(20000);
    }

    //Spring普通线程池(简化)
    @Test
    public void testSpringThreadExecuteSimple(){
        for (int i = 0; i < 10; i++) {
            alphaService.execute01();
        }
        sleep(10000);
    }

    //Spring定时任务线程池(简化)
    @Test
    public void testSpringThreadScheduleSimple(){
        sleep(20000);
    }
}
