package com.seb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.seb.repository")
public class SebApplication {
    public static void main(String[] args) {
        SpringApplication.run(SebApplication.class, args);
    }
}
