package com.zFrameWork.zEngine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica la regla CORS a todas las rutas de la API
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*") // Permite cualquier puerto local (Vite 5173, 5174, CRA 3000, etc)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
