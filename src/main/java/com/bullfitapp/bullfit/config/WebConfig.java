package com.bullfitapp.bullfit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ComingSoonInterceptor comingSoonInterceptor;

    public WebConfig(ComingSoonInterceptor comingSoonInterceptor) {
        this.comingSoonInterceptor = comingSoonInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(comingSoonInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/welcome", "/login", "/register", "/error",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}
