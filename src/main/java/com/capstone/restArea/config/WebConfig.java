package com.capstone.restArea.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 1. "/api/"로 시작하는 모든 요청
                .allowedOrigins("http://localhost:5173") // 2. React 앱(port: 5173)의 요청을 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD"); // 3. 허용할 HTTP 메서드
    }
}
