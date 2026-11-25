package com.grad.course.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射：/uploads/** 指向项目根目录下的 uploads/。
 * 不指向用户主目录，换机器、换账号仍可访问；种子图片与上传文件共用同一目录。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolute)
                .setCachePeriod(3600);
    }
}
