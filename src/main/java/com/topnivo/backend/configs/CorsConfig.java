package com.topnivo.backend.configs;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:5173")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173", "https://topnivo.netlify.app", "https://crystalgreengold.com", "https://www.crystalgreengold.com", "https://topnivo.org", "https://www.topnivo.org")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(60000);
    }
}