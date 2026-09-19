package com.hkd.mcptool;

import com.hkd.model.EmailInfo;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class EmailTool {

    //在spring框架中有一个邮箱发送的核心接口

    private final JavaMailSender mailSender;
    private final String fromName;


    public EmailTool(JavaMailSender mailSender,
                     @Value("${spring.mail.username}") String fromName) {
        this.mailSender = mailSender;
        this.fromName = fromName;
    }


    //查询邮箱：直接返回配置里的发件邮箱，避免把地址写死在代码里
    @Tool(description = "查询发件邮箱地址")
    public String getEmail() {
        return fromName;
    }


    /*
     给指定的邮箱发送邮件内容
     * 到实体Email中添加邮件内容
     */
//    @Tool(description = "给指定邮箱发送邮件,email为收件人邮箱,subject 为邮箱主题,message 为邮件内容")
//    public void sendEmailMessage(EmailInfo email) {
//        log.info("======调用MCP邮箱发送的工具=========");
//        log.info(String.format("邮箱的参数email:{}", email.getEmail()));
//
//        if(email== null || email.getEmail()==null || email.getEmail().isEmpty()){
//            log.error("收件人邮箱为空");
//           return;
//        }
//        try{
//            //创建邮件 javaEmail 原生邮件消息对象
//            MimeMessage mimeMessage= mailSender.createMimeMessage();
//            // 采用spring封装的工具类简化邮件设置
//            MimeMessageHelper helper=new MimeMessageHelper(mimeMessage);
//            helper.setFrom(fromName);//发件人
//            helper.setTo(email.getEmail()); //收件人
//            helper.setSubject(email.getSubject()); //主题
//            //这个地方转换为md文档 开启html模式，用于渲染md内容
//            helper.setText(email.getMessage()); //内容
//            mailSender.send(mimeMessage);
//            log.info("邮件发送成功，收件人：{}",email.getEmail());
//
//
//        }catch (Exception e){
//        log.error("邮件发送失败，报错信息是：{}",e.getMessage());
//
//        }
//    }

    @Tool(description = "给指定邮箱发送邮件信息,email为收件人邮箱,subject为邮件标题,message为邮件内容")
    public void sendEmailMessage(EmailInfo emails){
        log.info("============调用MCP邮件发送工具=========");
        log.info("发送邮件信息,收件人邮箱:{}",emails.getEmail());

        if (emails==null || emails.getEmail() ==null||emails.getEmail().isEmpty()){
            log.error("收件人邮箱为空");
            return ;
        }
        try{
            //创建邮件 javaMail
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            //封装的工具类i目的为了简化邮件设置
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            //设置收件人
            helper.setFrom(fromName);
            helper.setTo(emails.getEmail());
            //设置邮件标题
            helper.setSubject(emails.getSubject());
            //设置邮件内容
            helper.setText(convertToHtml(emails.getMessage()),true);
            mailSender.send(mimeMessage);
            log.info("邮件发送成功，收件人：{}",emails.getEmail());

        }catch (Exception e){
            log.error("=======邮件发送失败，报错信息：{}========",e.getMessage());
        }

    }

    //md格式转换为html
    /*
      将mddata 格式先转成文本
      在html格式转换为Html格式
     */
    public static String convertToHtml(String md){
        MutableDataSet mdData = new MutableDataSet();
        Parser parser=Parser.builder(mdData).build();
        HtmlRenderer htmlRenderer= HtmlRenderer.builder(mdData).build();

        return htmlRenderer.render(parser.parse(md));
    }


}


