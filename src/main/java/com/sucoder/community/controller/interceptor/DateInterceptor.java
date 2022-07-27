package com.sucoder.community.controller.interceptor;

import com.sucoder.community.entity.User;
import com.sucoder.community.service.DateService;
import com.sucoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DateInterceptor implements HandlerInterceptor {

    @Autowired
    private DateService dateService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //统计uv
        String ip = request.getRemoteHost();
        dateService.recordUV(ip);
        //统计DAU
        User user = hostHolder.getUser();
        if (user!=null){
            dateService.recordDAU(user.getId());
        }
        return true;
    }
}
