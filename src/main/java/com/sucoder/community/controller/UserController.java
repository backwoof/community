package com.sucoder.community.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.sucoder.community.annotation.LoginRequired;
import com.sucoder.community.entity.User;
import com.sucoder.community.service.FollowService;
import com.sucoder.community.service.LikeService;
import com.sucoder.community.service.UserService;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.CommunityUtil;
import com.sucoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;


    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(Model model){
        //上传文件名称
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        //生成上传凭证
        Auth auth = Auth.create(accessKey,secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);
        return "/site/setting";
    }

    //更新头像的路径
    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1,"文件名不能为空");
        }
        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(),url);
        return CommunityUtil.getJSONString(0);
    }

    //废弃
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if (headerImage==null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));//获取图片文件名后缀
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        filename = CommunityUtil.generateUUID()+suffix;
        //确定文件存放路径
        File dest = new File(uploadPath+"/"+filename);
        try {
            headerImage.transferTo(dest);//存储文件
        } catch (IOException e) {
            logger.error("上传文件失败"+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常"+e);
        }
        //更新当前用户头像的路径
        User user = hostHolder.getUser();
        String headerUrl = domain+contextPath+"/user/header/"+filename;
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";
    }

    //废弃
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName = uploadPath+"/"+fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
                ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b= fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败"+e.getMessage());
        }
    }

    @RequestMapping(path = "/password",method = RequestMethod.POST)
    public String updatePw(Model model,String password1,String password2,String password3){
        User user = hostHolder.getUser();
        if (password1 == null || password2 ==null || password3 == null){
            return "/site/setting";
        }
        password1 = CommunityUtil.md5(password1 + user.getSalt());
        if (!password1.equals(user.getPassword())) {
            model.addAttribute("mistake1","原密码输入有错");
            return "/site/setting";
        }
        password2 = CommunityUtil.md5(password2 + user.getSalt());
        if (password1.equals(password2)){
            model.addAttribute("mistake2","新密码与原密码重复");
            return "/site/setting";
        }
        password3 = CommunityUtil.md5(password3 + user.getSalt());
        if (!password2.equals(password3)){
            model.addAttribute("mistake3","两次输入的密码不一致");
            return "/site/setting";
        }
        userService.changePassword(user.getId(),password2);
        return "redirect:/index";
    }

    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw  new RuntimeException("该用户不存在!");
        }
        //用户
        model.addAttribute("user",user);
        //点赞数量
        int userLikeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("userLikeCount",userLikeCount);
        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followerCount",followerCount);
        //是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), userId, ENTITY_TYPE_USER);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }
}
