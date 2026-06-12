package com.ssafy.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@MapperScan("com.ssafy.ai.dao")
public class SpringAiProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiProjectApplication.class, args);

    }

}
