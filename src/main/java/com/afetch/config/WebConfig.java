package com.afetch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // All media is now served from Google Cloud Storage
    // No local resource handlers needed
}
