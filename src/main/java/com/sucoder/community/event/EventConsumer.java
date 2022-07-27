package com.sucoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.sucoder.community.entity.DiscussPost;
import com.sucoder.community.entity.Event;
import com.sucoder.community.entity.Message;
import com.sucoder.community.service.DiscussPostService;
import com.sucoder.community.service.ElasticsearchService;
import com.sucoder.community.service.MessageService;
import com.sucoder.community.util.CommunityConstant;
import com.sucoder.community.util.CommunityUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    @Autowired
    private MessageService messageService;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Value("${wk.image.command}")
    private String wkImageCommand;
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_LIKE,TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        //发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());


        Map<String,Object> map = new HashMap<>();
        map.put("userId",event.getUserId());
        map.put("entityType",event.getEntityType());
        map.put("entityId",event.getEntityId());

        if (!event.getData().isEmpty()){
            for (Map.Entry<String,Object> entry:event.getData().entrySet()){//****
                map.put(entry.getKey(),entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(map));
        messageService.addMessage(message);
    }
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        DiscussPost discussPost = discussPostService.findDiscussPost(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        elasticsearchService.deleteDiscussPost(discussPostService.findDiscussPost(event.getEntityId()));
    }

    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");

        String cmd = wkImageCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"+fileName+suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功："+wkImageCommand);
        } catch (IOException e) {
            logger.info("生成长图失败："+e.getMessage());
        }

        //启动定时器，监视图片，当图片生成后，上传至七牛云
        UploadTask uploadTask = new UploadTask(fileName,suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask,500);
        uploadTask.setFuture(future);
    }

    class UploadTask implements Runnable{
        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //future：启动任务的返回值
        private Future future;
        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public void setFuture(Future future) {
            this.future = future;
        }

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            // 生成失败
            if (System.currentTimeMillis()-startTime>30000){
                logger.error("执行时间过长，终止任务"+fileName);
                future.cancel(true);
                return;
            }
            //上传失败
            if (uploadTimes>=3){
                logger.error("执行时间过长，终止任务"+fileName);
                future.cancel(true);
                return;
            }

            String path = wkImageStorage + "/" +fileName+suffix;
            File file = new File(path);
            if (file.exists()){
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes,fileName));
                //设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                //生成上传凭证
                Auth auth = Auth.create(accessKey,secretKey);
                String uploadToken = auth.uploadToken(shareBucketName,fileName,3600,policy);
                //指定上传的机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
                try {
                    //开始上传图片
                    Response response = manager.put(
                            path,fileName,uploadToken,null,"image/"+suffix,false
                    );
                    //处理响应结果
                    JSONObject jsonObject = JSONObject.parseObject(response.bodyString());
                    if (jsonObject==null || jsonObject.get("code")==null || !jsonObject.get("code").toString().equals("0")){
                        logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                    }else {
                        logger.info(String.format("第%d次上传成功[%s]",uploadTimes,fileName));
                        future.cancel(true);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%s]",uploadTimes,fileName));
                }
            }else {
                logger.info("等待图片生成: "+fileName);
            }
        }
    }

}
