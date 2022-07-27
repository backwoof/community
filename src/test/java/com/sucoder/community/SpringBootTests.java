package com.sucoder.community;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringBootTests {
    @BeforeClass
    public static void beforeClass(){
        System.out.println("beforeClass");
    }
    @AfterClass
    public static void afterClass(){
        System.out.println("afterClass");
    }

    @Before
    public void before(){
        System.out.println("before");
    }
    @After
    public void after(){
        System.out.println("after");
    }
    @Test
    public void test1(){
        System.out.println("test1");
    }
    @Test
        public void test2(){
            System.out.println("test2");
    }


}
