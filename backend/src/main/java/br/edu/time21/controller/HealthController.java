package br.edu.time21.controller;

public class HealthController {
    public String getStatus() {
        return "{\"status\":\"online\",\"message\":\"API TIME21 funcionando\"}";
    }
}