package com.guiavest.backend_api.controller;

import com.guiavest.backend_api.model.HealthResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
// Libera o CORS especificamente para as URLs do Live Server
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class HealthController {

    @GetMapping("/health")
    public HealthResponse checkHealth() {
        // Retorna o objeto, e o Spring converte automaticamente para JSON
        return new HealthResponse("UP", "Backend Spring Boot rodando na porta 8080!");
    }
}
