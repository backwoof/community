package com.sucoder.community.config;

import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    @Override
    public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                    "/user/setting","/user/upload","/discuss/add","/like","/comment/add/**",
                        "/letter/**","/notice/**","/follow","/unfollow"
                )
                .hasAnyAuthority(
                    AUTHORITY_USER,AUTHORITY_MODERATOR,AUTHORITY_ADMIN
                )
                .antMatchers("/discuss/top","/discuss/wonderful")
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers("/discuss/delete","/data/**","/actuator/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll()
                .and().csrf().disable();
        //权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    //没有登录时的处理
                    @Override
                    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)){//异步请求
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有登录"));
                        }else {//同步请求
                                httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    //权限不足时的处理
                    @Override
                    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = httpServletRequest.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)){//异步请求
                            httpServletResponse.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = httpServletResponse.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你的权限不够"));
                        }else {//同步请求
                            httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/denied");
                        }
                    }
                });
        //security底层默认会拦截/logout请求,进行退出处理
        //覆盖默认的逻辑才能执行自定义的退出代码
        http.logout().logoutUrl("/qipian");
    }
}
