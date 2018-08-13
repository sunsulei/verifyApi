package com.fzbx.api;

import com.fzbx.api.utils.VerifyCodeParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(new Class[]{VerifyCodeParser.class, ApiApplication.class}, args);
    }
}
