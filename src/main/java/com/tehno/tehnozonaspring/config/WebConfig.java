package com.tehno.tehnozonaspring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4200",
                        "https://www.tehnozona.rs")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @org.springframework.beans.factory.annotation.Value("${category.images.path:category-images}")
    private String storagePath;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String path = new File(storagePath).getAbsolutePath();
        registry.addResourceHandler("/api/images/categories/**")
                .addResourceLocations("file:" + path + "/");
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setProperty("relaxedPathChars", "[]\\");
            connector.setProperty("relaxedQueryChars", "[]\\");
        });
    }
}
