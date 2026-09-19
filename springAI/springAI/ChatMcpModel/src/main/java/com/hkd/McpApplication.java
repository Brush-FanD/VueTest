package com.hkd;


import com.hkd.mcptool.DateTool;
import com.hkd.mcptool.EmailTool;
import com.hkd.mcptool.MathTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }

    //需要mcp注册一下

    @Bean
    public ToolCallbackProvider getToolMcp(MathTool mathTool, DateTool dateTool,
                                           EmailTool emailTool){
        /*
        ToolCallbackProvider:
        mcp协议的工具回调的接口，作用就是告诉AI框架有哪些工具可以调用 工具里面有哪些执行
        MethodToolCallbackProvider：
         mcp内置的实现类，自动扫描普通java类中注解@Tool
         toolObjects：需要注册的工具类 解析对应的tool注解 写的工具类 MathTool
         */



        return MethodToolCallbackProvider.builder().toolObjects(mathTool,dateTool
                ,emailTool).build();
    }
}

