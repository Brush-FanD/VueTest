package com.hkd.mcptool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class DateTool {

    @Tool(description = "获取当前时间")
    public String getCurrentDate(){
        log.info("=====调用MCP工具类获取当前系统时间====");
        String currentTime = String.format("当前时间为：%s",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return currentTime;
    }
    @Tool(description = "根据城市名称和时区id获取当前时间")
    public String GetCurrentDateByZoneId(String cityName,String zoneId){
        log.info("=====调用MCP工具类获取城市的当前时间====");
        log.info("========参数cityName %s========",cityName);
        log.info("========参数zoneId %s========",zoneId);

        //根据城市的名称获取时区
        ZoneId zone = ZoneId.of(zoneId);
        //获取当前时区的时间
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zone);
        String currentTime = String.format("当前时间为：%s",
                zonedDateTime.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return currentTime;
    }
}

