package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/CSS/**")
                .addResourceLocations("classpath:/static/CSS/");
        registry.addResourceHandler("/IMG/**")
                .addResourceLocations("classpath:/static/IMG/");
        registry.addResourceHandler("/JS/**")
                .addResourceLocations("classpath:/static/JS/");
    }
}