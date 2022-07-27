package com.sucoder.community.service;

import com.sucoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DateService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    //将指定的ip计入UV
    public void recordUV(String ip){
        String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey,ip);
    }

    //统计指定日期范围内的UV
    public long countUV(Date start,Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //整理日期范围内的key
        List<String> list = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)){
            String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(calendar.getTime()));
            list.add(uvKey);
            calendar.add(Calendar.DATE,1);
        }
        //合并这些数据
        String uvKeys = RedisKeyUtil.getUVKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
        redisTemplate.opsForHyperLogLog().union(uvKeys,list.toArray());
        return redisTemplate.opsForHyperLogLog().size(uvKeys);
    }

    //将指定用户计入DAU
    public void recordDAU(int userId){
        String dauKey = RedisKeyUtil.getDAU(simpleDateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey,userId,true);
    }

    //统计指定日期范围内的DAU
    public long countDAU(Date start, Date end){
        if (start == null || end == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        List<byte[]> list = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after((Date) end)){
            String dau = RedisKeyUtil.getDAU(simpleDateFormat.format(calendar.getTime()));
            list.add(dau.getBytes());
            calendar.add(Calendar.DATE,1);
        }
        //进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                String dau = RedisKeyUtil.getDAU(simpleDateFormat.format(start), simpleDateFormat.format(end));
                redisConnection.bitOp(RedisStringCommands.BitOperation.OR,dau.getBytes(),list.toArray(new byte[0][0]));
                return redisConnection.bitCount(dau.getBytes());
            }
        });
    }
}
