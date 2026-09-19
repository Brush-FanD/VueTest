package com.hkd.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // AI 偶尔会多塞字段，忽略未知字段避免反序列化失败
public class EmailInfo {
    //toolParam 标记该字段为Ai工具的入参项
    @ToolParam(description = "收件人邮箱", required = true)
    private String email;
    @ToolParam(description = "发送邮件的主题/标题", required = true)
    private String subject;
    @ToolParam(description = "发送邮件的内容", required = true)
    private String message;
}
