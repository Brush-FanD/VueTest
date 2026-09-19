package com.hkd.mcptool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MathTool {

    @Tool(description = "加法运算两个数相加和")
    public Integer addNumber(Integer a, Integer b) {
        log.info("调用MCP工具进行加法运算。。。");
        return a + b;
    }

    @Tool(description = "减法运算两个数相减差")
    public Integer subNumber(Integer a, Integer b) {
        log.info("调用MCP工具进行减法运算。。。");
        return a - b;
    }

    @Tool(description = "乘法运算两个数相乘积")

    public Integer mulNumber(Integer a, Integer b) {
        log.info("调用MCP工具进行乘法运算。。。");
        return a * b;
    }

    @Tool(description = "除法运算两个数相除商")
    public  Integer divNumber(Integer a, Integer b) {
        log.info("调用MCP工具进行除法运算。。。");
        return a / b;
    }


}
