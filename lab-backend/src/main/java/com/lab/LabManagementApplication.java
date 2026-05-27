package com.lab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lab.mapper")
public class LabManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabManagementApplication.class, args);
    }
}
