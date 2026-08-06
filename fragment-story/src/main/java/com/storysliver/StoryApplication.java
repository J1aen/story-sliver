package com.storysliver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.storysliver.mapper")
public class StoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoryApplication.class, args);
    }
}
