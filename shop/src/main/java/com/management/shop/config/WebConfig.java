package com.management.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig {

/*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://myshop360.s3-website.eu-north-1.amazonaws.com","http://localhost:3000","http://localhost:3000","null") // null for file:// origins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
*/

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("https://d8zu3msiux8e.cloudfront.net","https://d1q2sagaqur8v4.cloudfront.net", "https://d1v9gn9hmlq0vw.cloudfront.net", "http://shopapp-web.s3-website.eu-north-1.amazonaws.com","http://shopapp-mobile.s3-website.eu-north-1.amazonaws.com","http://myshop360.s3-website.eu-north-1.amazonaws.com",  "https://clearbill.store",  "https://web.clearbill.store", "https://m.clearbill.store", "http://myshop360-mobile.s3-website.eu-north-1.amazonaws.com","http://localhost:3000","http://localhost:3001","null","http://localhost:6062"));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
