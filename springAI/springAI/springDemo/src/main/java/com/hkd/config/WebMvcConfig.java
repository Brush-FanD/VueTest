package com.hkd.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Web 层统一配置：上传文件的静态资源映射 + 跨域规则。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String FILE_URL_PATTERN = "/files/**";

    private final String uploadDir;
    private final List<String> allowedOrigins;

    public WebMvcConfig(@Value("${app.upload-dir:./uploads}") String uploadDir,
                        @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.uploadDir = uploadDir;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * 把上传目录挂成静态资源：视频/封面的真实访问路径是 {context-path}/files/xxx
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(FILE_URL_PATTERN).addResourceLocations(location);
    }

    /**
     * 前端与后端不同源时才需要跨域；开发环境走 Vite 代理其实用不到，
     * 留在这里是为了直接访问后端 8123 端口调试时也能正常请求。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
