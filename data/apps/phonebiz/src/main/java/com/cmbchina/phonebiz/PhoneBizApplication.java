package com.cmbchina.phonebiz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cmbchina.phonebiz.mapper")
public class PhoneBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhoneBizApplication.class, args);
    }
}
