package com.sucoder.community;

import com.sucoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTest {
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testSentMail(){
        mailClient.sendMail("2918859226@qq.com","SpringMail","hahahaha");
    }

    @Test
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","suchangbo");//username是需要传入模板的参数名
        String process = templateEngine.process("/mail/demo", context);
        System.out.println(process);

        mailClient.sendMail("2918859226@qq.com","thymeleaf",process);
    }
}
