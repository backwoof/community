package com.sucoder.community.controller.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AlphaInterceptor implements HandlerInterceptor {//拦截器
    private static final Logger logger = LoggerFactory.getLogger(AlphaInterceptor.class);

    //在controller前面执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("preHandle: "+handler.toString());
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    //在controller之后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        logger.debug("postHandle: "+handler.toString());
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    //在TemplateEngine之后执行

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        logger.debug("afterCompletion: "+handler.toString());
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
