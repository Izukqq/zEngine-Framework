package com.zFrameWork.zEngine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZEngineApplication.class, args);
        System.out.println("=================================================");
        System.out.println("🌟 zEngine API Server Booted");
        System.out.println("Esperando comandos de ejecución desde el Frontend (React)...");
        System.out.println("API REST: http://localhost:8080/api/strategies");
        System.out.println("=================================================");
    }
}