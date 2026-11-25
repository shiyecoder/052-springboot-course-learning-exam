package com.grad.course;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 沉浸式剧场场次预约与门店管理系统启动类。
 * Mapper 统一扫描 com.grad.course.mapper，避免每个接口重复加 @Mapper。
 */
@SpringBootApplication
@MapperScan("com.grad.course.mapper")
public class CourseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourseApplication.class, args);
    }
}
